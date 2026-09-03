package com.travelcompanion.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.travelcompanion.app.data.preferences.ParticipantPreferences
import com.travelcompanion.app.data.trip.AssetTripRepository
import com.travelcompanion.app.data.trip.TripRepository
import com.travelcompanion.app.service.playback.DataStorePlaybackPositionStore
import com.travelcompanion.app.service.playback.Media3AudioEngine
import com.travelcompanion.app.service.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_state",
)

/**
 * Manual dependency wiring.
 *
 * A DI library would not earn its keep at this size (D001, and the brief lists
 * DI as an open choice). What matters is the lifetime: [PlaybackController]
 * is application-scoped so audio survives navigation and configuration change.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val tripRepository: TripRepository = AssetTripRepository(appContext)

    val participantPreferences = ParticipantPreferences(appContext)

    /**
     * Main-thread scope: Media3 controllers are single-threaded and the
     * position ticker updates UI state.
     */
    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioEngine = Media3AudioEngine(appContext)

    val playbackController = PlaybackController(
        engine = audioEngine,
        positions = DataStorePlaybackPositionStore(appContext.playbackDataStore),
        scope = playbackScope,
    )
}

class TravelCompanionApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** The container for this process. */
val Context.appContainer: AppContainer
    get() = (applicationContext as TravelCompanionApp).container
