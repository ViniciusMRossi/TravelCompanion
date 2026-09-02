package com.travelcompanion.app.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Product-facing boundary for group synchronization.
 *
 * Firebase is intentionally not wired in Phase 0.
 * Local playback must never depend on this repository being available.
 */
interface GroupSyncRepository {
    val state: Flow<GroupSyncState>

    suspend fun connect(participantId: String)
    suspend fun disconnect()
}

data class GroupSyncState(
    val status: Status = Status.Disabled,
) {
    enum class Status {
        Disabled,
        Connecting,
        Synchronized,
        Reconnecting,
        Offline,
    }
}

class NoOpGroupSyncRepository : GroupSyncRepository {
    override val state: Flow<GroupSyncState> =
        flowOf(GroupSyncState(GroupSyncState.Status.Disabled))

    override suspend fun connect(participantId: String) = Unit
    override suspend fun disconnect() = Unit
}
