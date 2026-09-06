package com.travelcompanion.app

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.travelcompanion.app.data.memory.MemoryDatabase
import com.travelcompanion.app.data.memory.RoomMemoryRepository
import com.travelcompanion.app.data.preferences.ParticipantPreferences
import com.travelcompanion.app.data.sync.FirebaseGroupSyncRepository
import com.travelcompanion.app.data.sync.GroupSyncRepository
import com.travelcompanion.app.data.trip.AssetTripRepository
import com.travelcompanion.app.data.trip.TripRepository
import com.travelcompanion.app.data.walk.DataStoreStoryTriggerStore
import com.travelcompanion.app.service.location.FusedLocationSource
import com.travelcompanion.app.service.location.PassiveStoryDiscovery
import com.travelcompanion.app.service.location.PlayServicesStoryGeofences
import com.travelcompanion.app.service.playback.DataStorePlaybackPositionStore
import com.travelcompanion.app.service.playback.Media3AudioEngine
import com.travelcompanion.app.service.playback.PlaybackController
import android.os.SystemClock
import com.travelcompanion.app.service.memory.MediaAudioRecorder
import com.travelcompanion.app.service.memory.MediaMemoryPlayer
import com.travelcompanion.app.service.memory.MemoryController
import com.travelcompanion.app.service.walk.AndroidWalkPresence
import com.travelcompanion.app.service.sync.GroupSessionController
import com.travelcompanion.app.service.notification.CriticalAlertScheduler
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

    /**
     * Room, and only here.
     *
     * Phase 0 has carried "add Room when structured runtime persistence is
     * first needed" from the start. Until voice memories there was nothing to
     * query: everything was either a single value in DataStore or came from
     * the packaged trip. A list of past recordings, newest first, with an
     * author and a duration, is the first thing that is a table (D057).
     */
    private val memoryDatabase: MemoryDatabase by lazy {
        Room.databaseBuilder(appContext, MemoryDatabase::class.java, "memories.db").build()
    }

    /**
     * Application-scoped like playback and Walk Mode, and for a sharper
     * reason: a recording in progress cannot be made again, so it must not
     * belong to a screen that can be navigated away from.
     *
     * Elapsed time is measured on the monotonic clock and the recording's date
     * on the wall clock — a memory made across midnight, or while the phone
     * corrects its time, must not end up with a negative duration.
     */
    val memoryController = MemoryController(
        recorder = MediaAudioRecorder(appContext),
        memories = RoomMemoryRepository(memoryDatabase.memories()),
        playback = playbackController,
        player = MediaMemoryPlayer(),
        filesDir = appContext::getFilesDir,
        scope = playbackScope,
        now = SystemClock::elapsedRealtime,
        epochNow = System::currentTimeMillis,
    )

    /**
     * One record for both paths.
     *
     * `notifyOncePerTrip` means once per trip and not once per mechanism: a
     * story announced by a geofence must not announce itself again on the
     * walk, so Walk Mode and passive discovery read and write the same store.
     */
    private val storyTriggerStore = DataStoreStoryTriggerStore(appContext.walkDataStore)

    /** One notifier for both paths, for the same reason (D103). */
    private val walkPresence = AndroidWalkPresence(appContext)

    val walkModeController = WalkModeController(
        locationSource = FusedLocationSource(appContext),
        triggerStore = storyTriggerStore,
        playbackController = playbackController,
        presence = walkPresence,
        scope = playbackScope,
    )

    /**
     * The circles Android watches while nobody is walking.
     *
     * Application-scoped like the walk and the alarms, and for the same
     * reason: a geofence outlives every screen, and the receiver that answers
     * one needs this with no Activity existing at all.
     */
    val passiveStoryDiscovery = PassiveStoryDiscovery(
        trip = { runCatching { tripRepository.load() }.getOrNull() },
        triggerStore = storyTriggerStore,
        geofences = PlayServicesStoryGeofences(appContext),
        presence = walkPresence,
        // The running walk owns the decision, and holds the record in memory
        // from its start. Passive discovery keeps quiet while it does (D105).
        isWalkRunning = { walkModeController.state.value.isRunning },
    )

    /**
     * The packaged deadlines, registered with `AlarmManager`.
     *
     * Application-scoped for the same reason playback is: an alarm outlives
     * the screen that caused it to be scheduled, and a reboot receiver needs
     * it without an Activity existing at all.
     */
    val criticalAlertScheduler = CriticalAlertScheduler(appContext, tripRepository)
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
