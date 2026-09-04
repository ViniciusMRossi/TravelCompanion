package com.travelcompanion.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.travelcompanion.app.data.preferences.ParticipantPreferences
import com.travelcompanion.app.data.sync.FirebaseGroupSyncRepository
import com.travelcompanion.app.data.sync.GroupSyncRepository
import com.travelcompanion.app.data.trip.AssetTripRepository
import com.travelcompanion.app.data.trip.TripRepository
import com.travelcompanion.app.data.walk.DataStoreStoryTriggerStore
import com.travelcompanion.app.service.location.FusedLocationSource
import com.travelcompanion.app.service.playback.DataStorePlaybackPositionStore
import com.travelcompanion.app.service.playback.Media3AudioEngine
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.walk.AndroidWalkPresence
import com.travelcompanion.app.service.sync.GroupSessionController
import com.travelcompanion.app.service.walk.WalkModeController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_state",
)

private val Context.walkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "walk_state",
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

    /**
     * Application-scoped for the same reason as playback: a walk continues
     * with the screen off and through Activity recreation, and screen 07 is
     * what the traveller sees when they take the phone out, not what keeps
     * the walk running.
     */
    /**
     * The group's runtime state, behind the boundary D002 established.
     *
     * `groupId` is read from the packaged trip rather than from anything the
     * traveller enters: the participants already belong to this trip.
     */
    private fun groupSyncRepository(groupId: String): GroupSyncRepository =
        FirebaseGroupSyncRepository(appContext, groupId)

    @Volatile
    private var groupSession: GroupSessionController? = null

    /**
     * Application-scoped like playback and Walk Mode: a shared listen must
     * survive screen 09 being disposed and the Activity being recreated.
     *
     * One per process. The trip is loaded once and carries one `groupId`, so a
     * second call cannot mean a different group.
     */
    fun groupSessionController(
        groupId: String,
        participantId: () -> String?,
    ): GroupSessionController = groupSession ?: synchronized(this) {
        groupSession ?: GroupSessionController(
            sync = groupSyncRepository(groupId),
            playback = playbackController,
            scope = playbackScope,
            participantId = participantId,
        ).also { groupSession = it }
    }

    val walkModeController = WalkModeController(
        locationSource = FusedLocationSource(appContext),
        triggerStore = DataStoreStoryTriggerStore(appContext.walkDataStore),
        playbackController = playbackController,
        presence = AndroidWalkPresence(appContext),
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
