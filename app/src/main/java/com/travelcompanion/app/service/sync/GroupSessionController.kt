package com.travelcompanion.app.service.sync

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.GroupSyncRepository
import com.travelcompanion.app.data.sync.GroupSyncState
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.domain.sync.GroupPlayback
import com.travelcompanion.app.domain.sync.LocalPlayback
import com.travelcompanion.app.domain.sync.ServerClock
import com.travelcompanion.app.domain.sync.SyncCorrection
import com.travelcompanion.app.domain.sync.syncCorrection
import com.travelcompanion.app.service.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** What screens 08 and 09 render, with no transport vocabulary in it. */
data class GroupSessionState(
    val status: GroupSyncState.Status = GroupSyncState.Status.Disabled,
    val participants: List<GroupParticipant> = emptyList(),
    /** Counts 3, 2, 1 on screen 08; null once the shared start has happened. */
    val countdown: Int? = null,
    val sharedMediaId: String? = null,
) {
    val isStarting: Boolean get() = countdown != null

    /**
     * Whether to show the amber "could not synchronize" note on screen 09.
     *
     * Only meaningful once the traveller has asked to listen together — a
     * group that was never joined is not a group that failed.
     */
    val isDegraded: Boolean
        get() = sharedMediaId != null &&
            (status == GroupSyncState.Status.Reconnecting || status == GroupSyncState.Status.Offline)
}

/**
 * Keeps this phone's playback and the group's in step.
 *
 * Application-scoped for the same reason as playback and Walk Mode: the phone
 * goes in a pocket, and a shared listen must not depend on screen 09 being
 * composed.
 *
 * The rule this class exists to honour is that group sync never blocks local
 * behaviour (brief §3.3). It is visible in the shape of the code: every path
 * from the group into the player goes through [applyCorrection], and the only
 * thing an unreachable group produces is [SyncCorrection.None]. Nothing here
 * pauses, stops or unloads audio because the network went away.
 */
class GroupSessionController(
    private val sync: GroupSyncRepository,
    private val playback: PlaybackController,
    private val scope: CoroutineScope,
    private val participantId: () -> String?,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(GroupSessionState())
    val state: StateFlow<GroupSessionState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var heartbeatJob: Job? = null
    private var clock: ServerClock = ServerClock()

    /** When the server last acknowledged something from this phone, in device time. */
    private var lastAckAtMs: Long = 0L

    /** Screen 09 opening: start listening to the group, if there is one. */
    fun join() {
        if (observeJob?.isActive == true) return
        val id = participantId() ?: return
        observeJob = scope.launch {
            sync.connect(id)
            sync.state.collect(::onGroupState)
        }
        heartbeatJob = scope.launch {
            while (isActive) {
                beat()
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /** Screen 09 closing. Local playback is deliberately left running. */
    fun leave() {
        observeJob?.cancel()
        heartbeatJob?.cancel()
        observeJob = null
        heartbeatJob = null
        lastAckAtMs = 0L
        scope.launch { sync.disconnect() }
        _state.value = GroupSessionState()
    }

    /**
     * "Ouvir juntos": proposes a shared start a few seconds out.
     *
     * The anchor is server time plus a delay, never this device's wall clock,
     * so both phones compute the same moment even when their clocks disagree
     * (brief §6). The countdown on screen 08 is that delay made visible.
     */
    fun startTogether(mediaId: String, positionMs: Long) {
        val id = participantId() ?: return
        val startAt = clock.serverNow(now()) + COUNTDOWN_MS
        _state.update { it.copy(sharedMediaId = mediaId, countdown = COUNTDOWN_SECONDS) }

        scope.launch {
            sync.publish(
                GroupPlayback(
                    mediaId = mediaId,
                    positionMs = positionMs,
                    isPlaying = true,
                    anchorServerMs = startAt,
                    updatedBy = id,
                ),
            )
        }
    }

    /** Drives 3 → 2 → 1 → gone. Called once per second while starting. */
    fun tickCountdown() {
        _state.update { current ->
            val next = current.countdown ?: return@update current
            if (next <= 1) current.copy(countdown = null) else current.copy(countdown = next - 1)
        }
    }

    /**
     * One round trip to the group, and what it implies for the screen.
     *
     * Silence cannot be the signal. Two people listening to the same guide
     * write nothing for minutes on end, and on the device screen 09 duly
     * accused a perfectly reachable group of having failed after twenty quiet
     * seconds. Firebase cannot supply the signal either: with the radios off
     * and screen 09 open, `.info/connected` stayed silent for the full two
     * minutes it was watched. So this phone asks, and only an acknowledged
     * write counts as an answer (D039).
     *
     * Separate from the ticker that calls it so a test can drive it a beat at
     * a time instead of waiting.
     */
    suspend fun beat() {
        val acknowledged = withTimeoutOrNull(HEARTBEAT_TIMEOUT_MS) { sync.heartbeat() } == true
        if (acknowledged) {
            lastAckAtMs = now()
            _state.update { it.moveTo(GroupSyncState.Status.Synchronized, from = GroupSyncState.Status.Reconnecting) }
        }
        checkStaleness()
    }

    /** Moves the screen to the amber state once the round trips stop landing. */
    fun checkStaleness() {
        if (!isStale()) return
        _state.update { it.moveTo(GroupSyncState.Status.Reconnecting, from = GroupSyncState.Status.Synchronized) }
    }

    /**
     * Changes the group's status and this phone's own row together.
     *
     * They are one fact told twice on screen 09 — the note under the list and
     * the dot beside the traveller's name — so they cannot be updated
     * separately. Nothing else moves: a status change is not news about the
     * other participants, whose rows only the group's own snapshot can carry.
     */
    private fun GroupSessionState.moveTo(
        status: GroupSyncState.Status,
        from: GroupSyncState.Status,
    ): GroupSessionState {
        if (this.status != from) return this
        val id = participantId()
        val mine = if (status == GroupSyncState.Status.Synchronized) {
            ParticipantSync.Synchronized
        } else {
            ParticipantSync.Reconnecting
        }
        return copy(
            status = status,
            participants = participants.map { if (it.id == id) it.copy(sync = mine) else it },
        )
    }

    /**
     * True once this phone has stopped reaching the server.
     *
     * Never true before the first acknowledgement: a group that was never
     * reached is not a group that went away.
     */
    private fun isStale(): Boolean =
        lastAckAtMs != 0L && now() - lastAckAtMs > STALE_AFTER_MS

    private fun onGroupState(group: GroupSyncState) {
        clock = group.clock
        _state.update { current ->
            current.copy(
                status = liveness(group.status),
                participants = withSelf(group.participants),
                sharedMediaId = group.playback?.mediaId ?: current.sharedMediaId,
            )
        }
        applyCorrection(group)
    }

    /**
     * The repository's own verdict, downgraded when the round trips have
     * stopped landing.
     *
     * Firebase raises events for this phone's own writes before they reach
     * anyone, so an arriving snapshot on its own is not evidence that the
     * group is still there.
     */
    private fun liveness(reported: GroupSyncState.Status): GroupSyncState.Status =
        if (reported == GroupSyncState.Status.Synchronized && isStale()) {
            GroupSyncState.Status.Reconnecting
        } else {
            reported
        }

    /**
     * The one place the group is allowed to touch the player.
     *
     * Every branch either moves playback or leaves it alone. There is no
     * branch that stops it, which is what makes "sync never blocks local
     * behaviour" a property of the code rather than a promise.
     */
    private fun applyCorrection(group: GroupSyncState) {
        val local = LocalPlayback(
            mediaId = playback.state.value.mediaId,
            positionMs = playback.state.value.positionMs,
            isPlaying = playback.state.value.isPlaying,
            durationMs = playback.state.value.durationMs,
        )
        // A group that is not live has nothing to say about this phone.
        val remote = group.playback.takeIf { group.isLive && !isStale() }

        when (val correction = syncCorrection(local, remote, group.clock.serverNow(now()))) {
            SyncCorrection.None -> Unit
            is SyncCorrection.Seek -> playback.seekTo(correction.positionMs)
            is SyncCorrection.Resume -> {
                playback.seekTo(correction.positionMs)
                playback.play()
            }
            SyncCorrection.Pause -> playback.pause()
            is SyncCorrection.Load -> {
                // Loading a different guide is the shared-start case, and the
                // screen that asked for it already owns the request; following
                // it blindly here would let the group replace what this phone
                // is listening to on its own.
                Unit
            }
        }
    }

    /**
     * This phone's own row, which the group's snapshot cannot supply.
     *
     * It reads as synchronized while the round trips are landing: the phone
     * knows its own playback without asking anyone. Once they stop landing it
     * reads as reconnecting like everyone else, because it is this phone that
     * lost the group — leaving it green above the amber note told the
     * traveller both things at once, which was observed on the device.
     */
    private fun withSelf(remote: List<GroupParticipant>): List<GroupParticipant> {
        val id = participantId() ?: return remote
        val mine = if (isStale()) ParticipantSync.Reconnecting else ParticipantSync.Synchronized
        return listOf(GroupParticipant(id, mine)) + remote.filterNot { it.id == id }
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val COUNTDOWN_MS = COUNTDOWN_SECONDS * 1_000L

        /**
         * Long enough that a hiccup does not flicker the screen, short enough
         * that two people standing together are told while it still matters.
         */
        const val STALE_AFTER_MS = 20_000L

        /** Four chances to be heard before the screen says anything. */
        const val HEARTBEAT_INTERVAL_MS = 5_000L

        /** Shorter than the interval, so beats cannot pile up on a dead link. */
        const val HEARTBEAT_TIMEOUT_MS = 4_000L
    }
}
