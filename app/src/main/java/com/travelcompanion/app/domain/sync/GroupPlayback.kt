package com.travelcompanion.app.domain.sync

/**
 * The shared playback state, as the group agrees on it.
 *
 * This is the whole payload the group exchanges: which packaged guide, where in
 * it, and from which moment. Never audio bytes, never trip content — every
 * phone already has the same file (brief §5).
 */
data class GroupPlayback(
    val mediaId: String,
    /** Position at [anchorServerMs], not "now". */
    val positionMs: Long,
    val isPlaying: Boolean,
    /**
     * Server time the position was taken at, or the agreed start moment for a
     * synchronized start. Server time, never a device wall clock (brief §6).
     */
    val anchorServerMs: Long,
    val updatedBy: String,
)

/** What the local player should do about the group, if anything. */
sealed interface SyncCorrection {

    /** Already close enough, or nothing to follow. */
    data object None : SyncCorrection

    /** Drift beyond tolerance: move to where the group is. */
    data class Seek(val positionMs: Long) : SyncCorrection

    /** A different guide than the one loaded, or none loaded yet. */
    data class Load(val mediaId: String, val positionMs: Long) : SyncCorrection

    /** The group paused and this phone has not. */
    data object Pause : SyncCorrection

    /** The group is playing and this phone is not. */
    data class Resume(val positionMs: Long) : SyncCorrection
}

/** The local side of the comparison, with no player type attached. */
data class LocalPlayback(
    val mediaId: String?,
    val positionMs: Long,
    val isPlaying: Boolean,
    /** 0 when the guide's length is not known yet. */
    val durationMs: Long = 0L,
)

/**
 * Where the group should be at [serverNowMs].
 *
 * `expected = position at the anchor + time elapsed since it`, per brief §5. A
 * paused group does not move, so its expected position is simply its own.
 */
fun expectedPosition(group: GroupPlayback, serverNowMs: Long): Long {
    if (!group.isPlaying) return group.positionMs
    val elapsed = serverNowMs - group.anchorServerMs
    // A start agreed for a moment that has not arrived yet is still at zero
    // elapsed rather than negative: this is the 3–2–1 window on screen 08.
    if (elapsed <= 0L) return group.positionMs
    return group.positionMs + elapsed
}

/**
 * Whether the listen this describes has already run past its own guide.
 *
 * Nothing writes a stop when a guide simply ends, and nothing writes one when
 * everybody walks away, so `playback` outlives the listen it describes: it
 * comes back the next day still saying "playing", with an anchor from
 * yesterday and an expected position hours past the end. Watched on two
 * devices — the node left by an earlier session made one phone join a listen
 * that was over, at 12:00 of a 12:00 guide (D045).
 *
 * [durationMs] is the guide's own length, which is packaged content. Zero
 * means it is not known here, and an unknown length ends nothing.
 */
fun isFinished(group: GroupPlayback, serverNowMs: Long, durationMs: Long): Boolean =
    durationMs > 0L && expectedPosition(group, serverNowMs) >= durationMs

/**
 * What to do when this phone and the group disagree.
 *
 * The answer is deliberately a value: "two participants diverge" is the
 * question this phase has to get right, and it is decided here with no
 * network, no Firebase and no player — a unit test can ask it directly.
 *
 * Nothing here can stop local audio. Every branch either leaves the player
 * alone or moves it; none of them stops playback because the group is
 * unreachable, which is why an absent [group] means [SyncCorrection.None]
 * rather than a pause (brief §3.3).
 */
fun syncCorrection(
    local: LocalPlayback,
    group: GroupPlayback?,
    serverNowMs: Long,
    driftToleranceMs: Long = DRIFT_TOLERANCE_MS,
): SyncCorrection {
    // No group state — not connected, not configured, or the group has never
    // played anything. Local playback carries on untouched.
    group ?: return SyncCorrection.None

    val expected = expectedPosition(group, serverNowMs)

    if (local.mediaId != group.mediaId) {
        return SyncCorrection.Load(group.mediaId, expected)
    }

    if (!group.isPlaying) {
        return if (local.isPlaying) SyncCorrection.Pause else SyncCorrection.None
    }

    // A start agreed for a moment that has not arrived yet moves nobody.
    //
    // During the 3–2–1 the group's position is where playback *will* begin,
    // not a claim about where anyone is now — and the audio does not stop
    // while the countdown runs. Measuring the two against each other makes the
    // gap widen by a second per second, so two of the three seconds are past
    // the tolerance and every snapshot inside the window drags the player back
    // to the anchor position. Loading is deliberately decided above this:
    // a phone that has to preload the guide should do so before the moment
    // arrives (brief §5), and only the moving of a player already in place is
    // held back.
    if (serverNowMs < group.anchorServerMs) return SyncCorrection.None

    // The group is asking for a point this guide does not have.
    //
    // Nothing publishes a stop when a guide simply ends, so a shared listen
    // that ran to the end leaves the group node saying "playing" for as long
    // as it stands. Its expected position keeps advancing past the guide, and
    // acting on that would drag the player back to the last frame and start it
    // again — once per heartbeat, forever. A guide that finished has finished.
    if (isFinished(group, serverNowMs, local.durationMs)) return SyncCorrection.None

    if (!local.isPlaying) return SyncCorrection.Resume(expected)

    val drift = kotlin.math.abs(local.positionMs - expected)
    // Small drift is left alone on purpose: correcting it would be audible as
    // a stutter, and this is synchronized narration, not multi-speaker music.
    return if (drift > driftToleranceMs) SyncCorrection.Seek(expected) else SyncCorrection.None
}

/**
 * How far apart two phones may drift before it is worth a seek.
 *
 * Two seconds is inaudible as a narration offset between two people walking
 * together, and well above the jitter of a database round trip.
 */
const val DRIFT_TOLERANCE_MS: Long = 2_000L

/**
 * Device-to-server clock offset (brief §6).
 *
 * Device wall clocks are not trusted for a synchronized start: two phones can
 * disagree by minutes. Everything that matters is computed in server time, and
 * this is the only place the two are related.
 */
data class ServerClock(val offsetMs: Long = 0L) {
    fun serverNow(deviceNowMs: Long): Long = deviceNowMs + offsetMs

    fun deviceTimeFor(serverMs: Long): Long = serverMs - offsetMs

    companion object {
        /** Offset from one observation of the server's own timestamp. */
        fun fromObservation(serverMs: Long, deviceMs: Long) = ServerClock(serverMs - deviceMs)
    }
}
