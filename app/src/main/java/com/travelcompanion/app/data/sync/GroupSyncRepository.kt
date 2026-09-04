package com.travelcompanion.app.data.sync

import com.travelcompanion.app.domain.sync.GroupPlayback
import com.travelcompanion.app.domain.sync.ServerClock
import kotlinx.coroutines.flow.Flow

/**
 * Product-facing boundary for group synchronization (D002).
 *
 * The contract is deliberately narrow: it carries live runtime state about who
 * is listening and where the shared guide is, and nothing else. Trip content
 * stays in the packaged `trip.json`, and audio stays on the device — what
 * crosses this boundary is playback state, never bytes (brief §5).
 *
 * Local playback must never depend on this repository being available. Every
 * implementation is allowed to fail, and failing means the group state goes
 * unavailable — not that anything stops playing (brief §3.3).
 */
interface GroupSyncRepository {
    val state: Flow<GroupSyncState>

    suspend fun connect(participantId: String)
    suspend fun disconnect()

    /**
     * Offers this phone's playback to the group.
     *
     * Best-effort by contract: a publish that does not arrive is not an error
     * the caller has to handle, because nothing local depends on it.
     */
    suspend fun publish(playback: GroupPlayback)

    /**
     * One round trip to the group, used as the liveness signal.
     *
     * Screens 08 and 09 have to tell the traveller when the group stopped
     * being reachable, and silence cannot carry that: while two people simply
     * listen, nobody writes anything for minutes at a time, so "no data
     * arrived" and "the network went away" look identical from here. A call
     * that only reports true once the server has acknowledged it tells them
     * apart (D039).
     *
     * Returns false rather than throwing, like everything else on this
     * boundary: an unreachable group is a state the screens render.
     */
    suspend fun heartbeat(): Boolean
}

/** One person in the trip group, as the screens show them. */
data class GroupParticipant(
    val id: String,
    val sync: ParticipantSync,
)

/**
 * A participant's state, in the two words the approved design uses.
 *
 * No infrastructure vocabulary reaches this type: there is no "connected",
 * no "socket", no host or client. Screens 08 and 09 render exactly these.
 */
enum class ParticipantSync { Synchronized, Reconnecting }

data class GroupSyncState(
    val status: Status = Status.Disabled,
    val participants: List<GroupParticipant> = emptyList(),
    /** What the group agrees is playing, or null when there is nothing to follow. */
    val playback: GroupPlayback? = null,
    /** Server-derived, because device wall clocks are not trusted (brief §6). */
    val clock: ServerClock = ServerClock(),
) {
    enum class Status {
        Disabled,
        Connecting,
        Synchronized,
        Reconnecting,
        Offline,
    }

    /** Whether the group is currently something the screens can follow. */
    val isLive: Boolean get() = status == Status.Synchronized
}
