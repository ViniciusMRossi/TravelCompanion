package com.travelcompanion.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.travelCompanionDataStore by preferencesDataStore(
    name = "travel_companion_preferences",
)

class ParticipantPreferences(
    private val context: Context,
) {
    val participantId: Flow<String?> = context.travelCompanionDataStore.data
        .map { preferences -> preferences[PARTICIPANT_ID] }

    suspend fun setParticipant(id: String) {
        context.travelCompanionDataStore.edit { preferences ->
            preferences[PARTICIPANT_ID] = id
        }
    }

    suspend fun clearParticipant() {
        context.travelCompanionDataStore.edit { preferences ->
            preferences.remove(PARTICIPANT_ID)
        }
    }

    private companion object {
        val PARTICIPANT_ID = stringPreferencesKey("participant_id")
    }
}
