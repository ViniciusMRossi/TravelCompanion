package com.travelcompanion.app.data.memory

import kotlinx.coroutines.flow.Flow

/**
 * One saved voice memory, as the app thinks about it.
 *
 * No title and no text: voice is the primary format and the approved sheet is
 * explicit that there is no text field. What a memory carries besides its
 * audio is what the app could fill in by itself — when, who and where.
 *
 * This never reaches `trip.json`. The schema guide lists voice memories among
 * the things that are runtime state rather than trip content, and nothing in
 * this phase writes to the package or to the group (D057).
 */
data class Memory(
    val id: String,
    val fileName: String,
    val recordedAtEpochMs: Long,
    val durationMs: Long,
    val participantId: String,
    val participantName: String,
    val cityName: String?,
    val placeName: String?,
)

/**
 * Where memories live, behind the narrowest interface that works.
 *
 * Three operations: keep one, read them back newest first, and remove one.
 * The screen needs nothing else, and neither does anything else in the app.
 */
interface MemoryRepository {
    fun memories(): Flow<List<Memory>>
    suspend fun save(memory: Memory)

    /** Removes the row. The audio file is the caller's to delete. */
    suspend fun delete(id: String)
}
