package com.travelcompanion.app.service.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Remembers where each audioguide was left.
 *
 * Runtime state, never written back into the packaged trip.
 */
interface PlaybackPositionStore {
    suspend fun load(mediaId: String): Long
    suspend fun save(mediaId: String, positionMs: Long)
    suspend fun clear(mediaId: String)
}

class DataStorePlaybackPositionStore(
    private val dataStore: DataStore<Preferences>,
) : PlaybackPositionStore {

    override suspend fun load(mediaId: String): Long =
        dataStore.data.first()[key(mediaId)] ?: 0L

    override suspend fun save(mediaId: String, positionMs: Long) {
        // A position at the very start is the same as no position at all, and
        // resuming a finished guide from its last second is worse than
        // restarting it — the caller decides, this only stores what it is told.
        dataStore.edit { it[key(mediaId)] = positionMs.coerceAtLeast(0L) }
    }

    override suspend fun clear(mediaId: String) {
        dataStore.edit { it.remove(key(mediaId)) }
    }

    private fun key(mediaId: String) = longPreferencesKey("playback_position_$mediaId")
}
