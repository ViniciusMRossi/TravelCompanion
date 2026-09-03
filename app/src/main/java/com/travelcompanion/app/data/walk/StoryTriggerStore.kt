package com.travelcompanion.app.data.walk

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.travelcompanion.app.domain.walk.StoryTriggerRecord
import kotlinx.coroutines.flow.first

/**
 * Which stories have already had their turn, per the brief's §20 record.
 *
 * Persisted rather than held in memory because `notifyOncePerTrip` means once
 * per trip: a story must not announce itself again because the process was
 * killed while the phone was in a pocket. Runtime state, never written back
 * into the packaged trip.
 */
interface StoryTriggerStore {
    suspend fun all(): Map<String, StoryTriggerRecord>
    suspend fun recordTriggered(storyId: String, atMillis: Long)
    suspend fun recordPlayed(storyId: String, atMillis: Long)
}

class DataStoreStoryTriggerStore(
    private val dataStore: DataStore<Preferences>,
) : StoryTriggerStore {

    override suspend fun all(): Map<String, StoryTriggerRecord> {
        val prefs = dataStore.data.first()
        return prefs.asMap().keys
            .mapNotNull { key -> key.name.removePrefixOrNull(TRIGGERED_PREFIX) }
            .associateWith { storyId ->
                StoryTriggerRecord(
                    storyId = storyId,
                    triggeredAt = prefs[triggeredKey(storyId)] ?: 0L,
                    playedAt = prefs[playedKey(storyId)],
                )
            }
    }

    override suspend fun recordTriggered(storyId: String, atMillis: Long) {
        dataStore.edit { prefs ->
            // First arrival wins: re-recording would move the timestamp and
            // hide that the story already fired.
            if (prefs[triggeredKey(storyId)] == null) prefs[triggeredKey(storyId)] = atMillis
        }
    }

    override suspend fun recordPlayed(storyId: String, atMillis: Long) {
        dataStore.edit { prefs -> prefs[playedKey(storyId)] = atMillis }
    }

    private companion object {
        const val TRIGGERED_PREFIX = "story_triggered_"
        const val PLAYED_PREFIX = "story_played_"

        fun triggeredKey(storyId: String) = longPreferencesKey("$TRIGGERED_PREFIX$storyId")
        fun playedKey(storyId: String) = longPreferencesKey("$PLAYED_PREFIX$storyId")

        fun String.removePrefixOrNull(prefix: String): String? =
            if (startsWith(prefix)) removePrefix(prefix) else null
    }
}
