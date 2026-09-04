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

    // The group is asking for a point this guide does not have.
    //
    // Nothing publishes a stop when a guide simply ends, so a shared listen
    // that ran to the end leaves the group node saying "playing" for as long
    // as it stands. Its expected position keeps advancing past the guide, and
    // acting on that would drag the player back to the last frame and start it
    // again — once per heartbeat, forever. A guide that finished has finished.
    if (local.durationMs > 0L && expected >= local.durationMs) return SyncCorrection.None

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
