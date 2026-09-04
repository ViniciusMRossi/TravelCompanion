package com.travelcompanion.app.service.walk

import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.walk.StoryTriggerStore
import com.travelcompanion.app.domain.walk.DeviceLocation
import com.travelcompanion.app.domain.walk.LocationQuality
import com.travelcompanion.app.domain.walk.StoryTriggerRecord
import com.travelcompanion.app.domain.walk.WalkPhase
import com.travelcompanion.app.service.location.LocationSource
import com.travelcompanion.app.service.playback.FakeAudioEngine
import com.travelcompanion.app.service.playback.InMemoryPlaybackPositionStore
import com.travelcompanion.app.service.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The orchestration around the pure decision, driven with the real packaged
 * trip.
 *
 * What is at risk here is not the arithmetic — that is covered in
 * `domain/walk` — but the wiring: a story that fires twice, a walk that ends
 * because location was refused, or an unpackaged audio asset stopping audio
 * that was already playing.
 */
class WalkModeControllerTest {

    private val content = packagedContent(exists = { true })
    private val walkId = "walk.sarajevo.historical"

    // The two real stops of the packaged Sarajevo walk.
    private val meetingPoint = GeoPoint(43.8590, 18.4257)
    private val bridgePoint = GeoPoint(43.8578, 18.4289)
    private val elsewhere = GeoPoint(43.8700, 18.4500)

    private val engine = FakeAudioEngine()
    private val playback = PlaybackController(
        engine = engine,
        positions = InMemoryPlaybackPositionStore(),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
    private val store = RecordingTriggerStore()
    private val presence = RecordingPresence()

    private fun controller(permission: Boolean = true) = WalkModeController(
        locationSource = FakeLocationSource(permission),
        triggerStore = store,
        playbackController = playback,
        presence = presence,
        scope = CoroutineScope(Dispatchers.Unconfined),
        now = { 1_000L },
    )

    private fun at(point: GeoPoint) = DeviceLocation(point, accuracyMeters = 5f)

    @Test
    fun `preparing loads the packaged walk without starting anything`() {
        val controller = controller()
        controller.prepare(content, walkId)

        val state = controller.state.value
        assertEquals(WalkPhase.Preparing, state.phase)
        assertEquals(2, state.stops.size)
        assertTrue(presence.started.isEmpty())
    }

    @Test
    fun `starting raises the persistent notification the walk needs`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()

        assertEquals(WalkPhase.Active, controller.state.value.phase)
        assertEquals(1, presence.started.size)
        assertEquals("Caminhada Histórica de Sarajevo", presence.started.single().first)
    }

    @Test
    fun `the same position repeated fires a story exactly once`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()

        repeat(5) { controller.onLocation(at(meetingPoint)) }

        assertEquals(listOf("story.meeting-of-cultures"), store.triggered)
        assertEquals(1, controller.state.value.visitedCount)
        assertEquals("story.latin-bridge", controller.state.value.nextStop?.storyId)
    }

    @Test
    fun `walking the route marks both stops in order`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()

        controller.onLocation(at(elsewhere))
        assertEquals(0, controller.state.value.visitedCount)

        controller.onLocation(at(meetingPoint))
        controller.onLocation(at(bridgePoint))

        assertEquals(
            listOf("story.meeting-of-cultures", "story.latin-bridge"),
            store.triggered,
        )
        assertEquals(2, controller.state.value.visitedCount)
        assertNull(controller.state.value.nextStop)
    }

    @Test
    fun `a story already triggered on a previous run does not fire again`() {
        store.preload("story.meeting-of-cultures")
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()

        controller.onLocation(at(meetingPoint))

        // Nothing new recorded: notifyOncePerTrip survived the process dying.
        assertEquals(emptyList<String>(), store.triggered)
        assertEquals(0, controller.state.value.visitedCount)
    }

    @Test
    fun `refused location leaves the walk running and audio untouched`() {
        val controller = controller(permission = false)
        controller.prepare(content, walkId)
        controller.start()

        val state = controller.state.value
        assertEquals(WalkPhase.Active, state.phase)
        assertEquals(LocationQuality.Denied, state.locationQuality)

        // No foreground service either. Android refuses one of type `location`
        // when the app holds no location permission, and that refusal is a
        // SecurityException that would take the whole app down — observed on
        // the device before this guard existed. With no location there is
        // nothing to keep in the foreground, so the walk runs without it.
        assertEquals(emptyList<Pair<String, String>>(), presence.started)
    }

    @Test
    fun `a story whose audio is not packaged does not disturb audio that is playing`() {
        // Only the Baščaršija guide is packaged; the stories' own audio is not.
        val bare = packagedContent(exists = { it.endsWith("bascarsija.prototype.wav") })
        val controller = WalkModeController(
            locationSource = FakeLocationSource(true),
            triggerStore = store,
            playbackController = playback,
            presence = presence,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        controller.prepare(bare, walkId)
        controller.start()

        playback.playAudioGuide(
            com.travelcompanion.app.service.playback.audioGuideRequest(bare, "ag.bascarsija")!!,
        )
        val playingBefore = playback.state.value.mediaId
        val prepareCountBefore = engine.prepareCount

        controller.onLocation(at(bridgePoint))

        // The stop still counts as reached, and the guide already playing was
        // neither replaced nor stopped (D021).
        assertEquals(1, controller.state.value.visitedCount)
        assertEquals(playingBefore, playback.state.value.mediaId)
        assertEquals(prepareCountBefore, engine.prepareCount)

        // Triggered, but never played: §20 keeps the two timestamps apart, and
        // claiming playedAt for audio that is not in the package would be a
        // record of something that did not happen.
        assertTrue(store.triggered.contains("story.latin-bridge"))
        assertEquals(emptyList<String>(), store.played)
    }

    @Test
    fun `finishing releases the walk and its notification`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()
        controller.finish()

        assertEquals(WalkPhase.Completed, controller.state.value.phase)
        assertEquals(1, presence.stopped)
    }

    private class FakeLocationSource(private val granted: Boolean) : LocationSource {
        override fun hasPermission(): Boolean = granted

        // Positions are fed straight to onLocation in these tests: the flow
        // plumbing needs a device, the decisions do not.
        override fun updates(): Flow<DeviceLocation> = emptyFlow()
    }

    private class RecordingTriggerStore : StoryTriggerStore {
        val triggered = mutableListOf<String>()
        val played = mutableListOf<String>()
        private val preloaded = mutableMapOf<String, StoryTriggerRecord>()

        fun preload(storyId: String) {
            preloaded[storyId] = StoryTriggerRecord(storyId, triggeredAt = 1L)
        }

        override suspend fun all(): Map<String, StoryTriggerRecord> = preloaded.toMap()

        override suspend fun recordTriggered(storyId: String, atMillis: Long) {
            triggered += storyId
        }

        override suspend fun recordPlayed(storyId: String, atMillis: Long) {
            played += storyId
        }
    }

    private class RecordingPresence : WalkPresence {
        val started = mutableListOf<Pair<String, String>>()
        val stories = mutableListOf<String>()
        var stopped = 0

        override fun startWalk(title: String, text: String) {
            started += title to text
        }

        override fun updateWalk(title: String, text: String) = Unit

        override fun stopWalk() {
            stopped++
        }

        override fun notifyStory(storyId: String, title: String, hook: String) {
            stories += storyId
        }
    }

    /**
     * Screen 10 exists for the *offered* case only.
     *
     * Both packaged stories declare `autoPlayInWalk`, so with automatic
     * stories on the decision is `PlayInWalk`: the story is already in the
     * traveller's ears and no page rises for it (D078). Turning the toggle off
     * on screen 06 is what makes the same arrival an offer.
     */
    @Test
    fun `a story that plays by itself raises no sheet`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()

        controller.onLocation(at(bridgePoint))

        assertNull(controller.state.value.pending)
        // It played instead: nothing was offered, so nothing is waiting.
        assertEquals(setOf("story.latin-bridge"), controller.state.value.playedStoryIds)
    }

    @Test
    fun `a story that is offered raises the sheet, with the distance it fired at`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.setAutomaticStories(false)
        controller.start()

        controller.onLocation(at(bridgePoint))

        val pending = controller.state.value.pending
        assertEquals("story.latin-bridge", pending?.storyId)
        // Standing on the trigger's own point, so a few metres at most.
        assertTrue("distance was ${pending?.distanceMeters}", pending!!.distanceMeters!! < 20)
    }

    @Test
    fun `Ouvir agora plays the story that was offered and closes the sheet`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.setAutomaticStories(false)
        controller.start()
        controller.onLocation(at(bridgePoint))

        controller.playPendingStory()

        assertNull(controller.state.value.pending)
        assertEquals(setOf("story.latin-bridge"), controller.state.value.playedStoryIds)
        assertTrue(store.played.contains("story.latin-bridge"))
    }

    /** "Depois" costs nothing: nothing plays, and nothing fires again. */
    @Test
    fun `Depois closes the sheet and plays nothing`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.setAutomaticStories(false)
        controller.start()
        controller.onLocation(at(bridgePoint))
        val playedBefore = store.played.size

        controller.dismissPendingStory()

        assertNull(controller.state.value.pending)
        assertTrue(controller.state.value.playedStoryIds.isEmpty())
        assertEquals(playedBefore, store.played.size)

        // And the same circle does not fire again: it was recorded as
        // triggered when it arrived.
        controller.onLocation(at(bridgePoint))
        assertNull(controller.state.value.pending)
    }

    /** Screen 11 needs both ends of the walk's own clock. */
    @Test
    fun `the walk stamps when it started and when it ended`() {
        val controller = controller()
        controller.prepare(content, walkId)
        controller.start()
        val started = controller.state.value.startedAtEpochMs

        controller.finish()

        assertEquals(1_000L, started)
        assertEquals(1_000L, controller.state.value.completedAtEpochMs)
        assertEquals(WalkPhase.Completed, controller.state.value.phase)
    }
}
