package com.travelcompanion.app.service.sync

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.GroupSyncRepository
import com.travelcompanion.app.data.sync.GroupSyncState
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.domain.sync.GroupPlayback
import com.travelcompanion.app.domain.sync.LocalPlayback
import com.travelcompanion.app.domain.sync.ServerClock
import com.travelcompanion.app.domain.sync.isFinished
import com.travelcompanion.app.domain.sync.SyncCorrection
import com.travelcompanion.app.domain.sync.syncCorrection
import com.travelcompanion.app.service.playback.AudioGuideRequest
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
    /**
     * The group is listening to one guide and this phone to another.
     *
     * Screen 09 has to say so. Two phones on different stories both reading
     * "Sincronizado" is the failure that has no symptom, and the traveller who
     * can act on it is the one holding the phone that is out of step (D042).
     */
    val isDiverged: Boolean = false,
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

    /**
     * The packaged trip, as a lookup, for as long as screens 08/09 are open.
     *
     * The controller carries no content of its own, and it needs two things
     * from it: whether the group's guide is playable here, and how long that
     * guide is — which is what says whether a shared listen is still running
     * or has been over since yesterday (D045).
     */
    private var guides: ((String) -> AudioGuideRequest?)? = null

    /**
     * The traveller's "Ouvir juntos", waiting for the group to answer.
     *
     * What the request means depends on what the group is doing, and on
     * arrival nobody knows that yet: the repository has been asked and has not
     * replied. So the intent is held until the group says something
     * conclusive, and [resolveArrival] decides then.
     */
    private var arrivalPending = false

    /**
     * One-shot permission for [applyCorrection] to put the group's guide on
     * this phone. Set only by [resolveArrival], and only for a phone that has
     * nothing playing.
     */
    private var invited = false

    /**
     * The last thing the group said.
     *
     * Kept because a snapshot is not what drives a correction — the beat is.
     * Two people simply listening write nothing but their own `seenAt`, which
     * this repository maps away, so consecutive snapshots arrive equal and the
     * state flow drops them. Watched on two devices: once the group settled,
     * the controller stopped hearing anything at all, and with it every
     * correction (D046).
     */
    private var lastGroup: GroupSyncState = GroupSyncState()

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
        lastGroup = GroupSyncState()
        guides = null
        arrivalPending = false
        invited = false
        scope.launch { sync.disconnect() }
        _state.value = GroupSessionState()
    }

    /**
     * Proposes a shared start a few seconds out.
     *
     * The anchor is server time plus a delay, never this device's wall clock,
     * so both phones compute the same moment even when their clocks disagree
     * (brief §6). The countdown on screen 08 is that delay made visible.
     *
     * Proposing is for when there is nothing to join. Whether that is the case
     * is [resolveArrival]'s decision, not this method's: publishing an anchor
     * over a listen that is already running moves everyone else to this
     * phone's position, once per visit to the screen (D042).
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

    /**
     * "Ouvir juntos", from screens 08 and 09.
     *
     * The prototype has one way into the shared listen — the button under
     * screen 07's transport — and it is the same button on both phones. So
     * arriving here is the traveller asking to listen with the group, and it
     * is that ask, not the group, that decides what happens to this player.
     * The group is never allowed to make the decision on its own (D037).
     *
     * [guides] resolves a packaged audioguide ID; it comes from the screen
     * because the trip is content and this controller carries none.
     */
    fun listenTogether(guides: (String) -> AudioGuideRequest?) {
        if (participantId() == null) return
        this.guides = guides
        arrivalPending = true
        // Deliberately not decided here. The snapshot in hand was left over
        // from before [join] asked, and reading "the group has nothing" off it
        // is how a phone ends up re-anchoring a listen that was already
        // running.
    }

    /**
     * What the traveller's ask turns out to mean, once the group has answered.
     *
     * Four outcomes, and the group's own state picks between them. Only the
     * third puts a guide on this phone, and only because the traveller just
     * asked for it.
     */
    private fun resolveArrival(group: GroupSyncState) {
        if (!arrivalPending) return
        // "Connecting" and "Disabled" are not answers: one is the question
        // still in flight, the other is a build with no group configured at
        // all (D034). Everything else is conclusive, including the ones that
        // mean the group is unreachable — an unreachable group must not leave
        // the traveller looking at a screen that never does anything.
        val answered = group.status == GroupSyncState.Status.Synchronized ||
            group.status == GroupSyncState.Status.Reconnecting ||
            group.status == GroupSyncState.Status.Offline
        if (!answered) return

        val shared = sharedListen(group)
        val localMediaId = playback.state.value.mediaId

        when {
            // Nobody is listening together yet: this is the phone that
            // proposes, which is the flow the prototype draws — screen 07's
            // "Ouvir juntos", then screen 08's 3–2–1.
            //
            // Unless it has nothing to propose with. A phone holding no guide,
            // with no listen to join, has not been answered yet in any useful
            // sense — so the ask is kept rather than spent, and it joins when
            // somebody does start one. Spending it here left the second phone
            // sitting on "Comece um audioguia" while the first counted down,
            // which is what two devices showed (D045).
            shared == null -> {
                val id = localMediaId ?: return
                arrivalPending = false
                startTogether(id, playback.state.value.positionMs)
            }

            // Already on the guide the group is listening to. Following is all
            // that is left, and the drift correction does it. Publishing again
            // would drag everyone else to this phone's position.
            shared.mediaId == localMediaId -> {
                arrivalPending = false
                _state.update { it.copy(sharedMediaId = shared.mediaId) }
            }

            // Nothing playing here, and the group is already listening. The
            // traveller asked to join them, so this phone takes their guide —
            // "both devices preload the local audio" (brief §5), which until
            // now nothing did.
            //
            // No countdown here. Screen 08 is a transition into playback, and
            // a group that is paused has nothing to transition into: counting
            // 3–2–1 at it ran the count out and dropped the traveller on
            // "Comece um audioguia" about the very listen they were joining.
            // The count now belongs to the load, in [loadIfInvited] (D044).
            localMediaId == null -> {
                arrivalPending = false
                invited = true
                _state.update { it.copy(sharedMediaId = shared.mediaId) }
            }

            // Playing something else. D037 stands: the group never replaces
            // it, and neither does this phone replace the group's. What was
            // missing is that the screen said nothing about it — see
            // [GroupSessionState.isDiverged].
            else -> {
                arrivalPending = false
                _state.update { it.copy(sharedMediaId = shared.mediaId) }
            }
        }
    }

    /**
     * Offers what this phone's transport just did to the group.
     *
     * Screen 09 is the shared player, and the approved design gives both
     * phones the same full transport with no host and no client — so pausing,
     * resuming and skipping there are things the group does, not things one
     * phone does behind the others' backs. Nothing published them, which left
     * [SyncCorrection.Pause] and [SyncCorrection.Resume] reachable only by
     * editing the database by hand; on two devices one traveller paused, the
     * other narrated on, and both screens read "Sincronizado" (D047).
     *
     * The payload is the same five fields as a shared start. The anchor is
     * now rather than a moment ahead: this is a correction to a listen that is
     * already running, not a new one being agreed.
     */
    fun shareLocalPlayback() {
        val id = participantId() ?: return
        val local = playback.state.value
        val mediaId = local.mediaId ?: return
        // Only for the listen this phone actually joined. A phone that is on
        // its own guide does not get to move the group (D042).
        if (_state.value.sharedMediaId != mediaId) return

        scope.launch {
            sync.publish(
                GroupPlayback(
                    mediaId = mediaId,
                    positionMs = local.positionMs,
                    isPlaying = local.isPlaying,
                    anchorServerMs = clock.serverNow(now()),
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
        // "Each device periodically computes expected playback position"
        // (brief §5) — periodically, which until now meant "whenever the
        // group's snapshot happened to differ". It stopped differing as soon
        // as two people settled into listening, and every correction stopped
        // with it (D046).
        applyCorrection(lastGroup)
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
        lastGroup = group
        // Before the screen is told anything: an arrival that resolves into a
        // proposal or an invitation has to be part of the same snapshot the
        // correction below acts on.
        resolveArrival(group)

        val shared = sharedListen(group)
        val localMediaId = playback.state.value.mediaId
        _state.update { current ->
            current.copy(
                status = liveness(group.status),
                participants = withSelf(group.participants),
                sharedMediaId = group.playback?.mediaId ?: current.sharedMediaId,
                isDiverged = shared != null &&
                    localMediaId != null &&
                    localMediaId != shared.mediaId,
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
        // A group that is not live — or whose listen is long over — has
        // nothing to say about this phone.
        val remote = sharedListen(group)

        when (val correction = syncCorrection(local, remote, group.clock.serverNow(now()))) {
            SyncCorrection.None -> Unit
            is SyncCorrection.Seek -> playback.seekTo(correction.positionMs)
            is SyncCorrection.Resume -> {
                playback.seekTo(correction.positionMs)
                playback.play()
            }
            SyncCorrection.Pause -> playback.pause()
            is SyncCorrection.Load -> loadIfInvited(correction, local, remote)
        }
    }

    /**
     * The group's listen, when there is one to follow.
     *
     * Null when the group is not live, when this phone has stopped reaching
     * it, and — the case two devices found — when the listen it describes is
     * already over. `playback` has no expiry: nothing writes a stop when a
     * guide ends or when everyone walks away, so the node from an earlier
     * session comes back still saying "playing". Acting on it made the first
     * traveller to tap "Ouvir juntos" join a dead listen instead of starting
     * one, and put the other phone at the last second of the guide (D045).
     */
    private fun sharedListen(group: GroupSyncState): GroupPlayback? {
        val live = group.playback?.takeIf { group.isLive && !isStale() } ?: return null
        val duration = declaredDurationMs(live.mediaId)
        return live.takeIf { !isFinished(it, group.clock.serverNow(now()), duration) }
    }

    /** The packaged length of a guide, or zero when this build cannot say. */
    private fun declaredDurationMs(mediaId: String): Long =
        (guides?.invoke(mediaId) as? AudioGuideRequest.Playable)?.declaredDurationMs ?: 0L

    /**
     * The only case in which the group's guide reaches this player.
     *
     * D037 is unchanged and is the first line: a phone that is playing
     * something is never swapped, whatever the group says and whoever asked.
     * What that decision was missing is its other half — a phone playing
     * *nothing*, whose traveller has just asked to listen with the group. That
     * ask is [invitation], it is set once per arrival, and it is spent here.
     *
     * The load goes through this method so [applyCorrection] stays the single
     * point of contact between the group and the player.
     */
    private fun loadIfInvited(
        load: SyncCorrection.Load,
        local: LocalPlayback,
        remote: GroupPlayback?,
    ) {
        if (local.mediaId != null) return
        // A group that is paused has nothing to join yet. The invitation is
        // kept rather than spent, so joining happens when they start again.
        if (remote?.isPlaying != true) return
        if (!invited) return
        // A guide this build does not carry is not playable, and saying so is
        // content's job, not the player's (D021). Asked before the invitation
        // is spent, so an unplayable answer costs nothing: the ask is still
        // good for whatever the group plays next (D044).
        val request = guides?.invoke(load.mediaId) as? AudioGuideRequest.Playable ?: return
        invited = false
        playback.playAudioGuide(request, startPositionMs = load.positionMs)
        // Screen 08, now that there is something to count down into. The
        // audio is already coming into step behind it — the group is mid-guide
        // and catching up is the point — so the three seconds are the
        // transition the approved design draws, not a delay before starting.
        _state.update { it.copy(countdown = COUNTDOWN_SECONDS) }
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
