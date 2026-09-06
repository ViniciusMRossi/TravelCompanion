package com.travelcompanion.app.service.location

import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.Story
import com.travelcompanion.app.data.walk.StoryTriggerStore
import com.travelcompanion.app.domain.walk.DeviceLocation
import com.travelcompanion.app.domain.walk.StoryTriggerRecord
import com.travelcompanion.app.domain.walk.distanceMeters
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wiring around the geofence, driven with the real packaged trip.
 *
 * §20's arithmetic is `StoryTriggeringTest`'s and is not repeated here. What
 * is at risk in this file is everything the wake-up adds around it: a story
 * that speaks twice, a circle left registered after it has spoken, a refused
 * permission taking something else down with it, and — the one that would be
 * invisible — the registration floor leaking into the radius the decision
 * applies (D103).
 */
class PassiveStoryDiscoveryTest {

    private val content = packagedContent(exists = { true })

    /** The two packaged triggers: 120 m at the crossing, 100 m at the bridge. */
    private val crossing = GeoPoint(43.8590, 18.4257)
    private val bridge = GeoPoint(43.8578, 18.4289)
    private val elsewhere = GeoPoint(43.8700, 18.4500)

    private val store = RecordingTriggerStore()
    private val geofences = RecordingGeofences()
    private val presence = RecordingPresence()

    private fun discovery(walkRunning: Boolean = false) = PassiveStoryDiscovery(
        trip = { content },
        triggerStore = store,
        geofences = geofences,
        presence = presence,
        isWalkRunning = { walkRunning },
        now = { 1_000L },
    )

    private fun at(point: GeoPoint) = DeviceLocation(point, accuracyMeters = 5f)

    @Test
    fun `every story with a trigger is registered, and only those`() = runBlocking {
        discovery().refresh()

        assertEquals(
            listOf("story.meeting-of-cultures", "story.latin-bridge"),
            geofences.registered.map { it.id },
        )
    }

    /**
     * The whole point of persisting the record: a phone restarted between two
     * visits must not announce the same story again.
     */
    @Test
    fun `a story already in the record is never registered and never notified`() = runBlocking {
        store.records["story.meeting-of-cultures"] =
            StoryTriggerRecord("story.meeting-of-cultures", triggeredAt = 1L)

        val discovery = discovery()
        discovery.refresh()
        discovery.onEntered(at(crossing))

        assertEquals(listOf("story.latin-bridge"), geofences.registered.map { it.id })
        assertTrue(geofences.cancelled.contains("story.meeting-of-cultures"))
        assertTrue(presence.notified.isEmpty())
    }

    /**
     * Play Services can deliver ENTER and then DWELL for the same circle, and
     * re-registration re-fires the initial trigger. One notification.
     */
    @Test
    fun `the same transition received twice notifies once`() = runBlocking {
        val discovery = discovery()
        repeat(4) { discovery.onEntered(at(crossing)) }

        assertEquals(listOf("story.meeting-of-cultures"), presence.notified.map { it.first })
        assertEquals(1, store.records.size)
    }

    /** The record is written before the notification, so a refused post still counts. */
    @Test
    fun `a story that fires is recorded and its circle comes down`() = runBlocking {
        val discovery = discovery()
        discovery.refresh()
        discovery.onEntered(at(bridge))

        assertEquals("story.latin-bridge", presence.notified.single().first)
        assertTrue(store.records.containsKey("story.latin-bridge"))
        assertEquals(listOf("story.latin-bridge"), geofences.cancelled)
    }

    /**
     * The registration floor is registration's, and the decision keeps the
     * content's own radius.
     *
     * The bridge declares 100 m and is registered at the 120 m floor. A fix
     * 110 m away is inside the circle Android watches and outside the circle
     * the content declares — so Android wakes the app and the app says
     * nothing. A floor that reached the decision would notify here, from a
     * block away, and no other test in this repository would see it.
     */
    @Test
    fun `the registration floor does not widen the radius the decision applies`() = runBlocking {
        val justOutside = GeoPoint(bridge.latitude + 0.00099, bridge.longitude)
        val metres = distanceMeters(justOutside, bridge)
        assertTrue("fixture drifted: $metres m", metres > 100.0 && metres < 120.0)

        val discovery = discovery()
        discovery.refresh()
        discovery.onEntered(at(justOutside))

        assertEquals(120f, geofences.radiusOf("story.latin-bridge"))
        assertTrue(presence.notified.isEmpty())
        assertTrue(store.records.isEmpty())
    }

    /** A wake-up somewhere else is silence, not an error. */
    @Test
    fun `a transition with nothing in range decides nothing`() = runBlocking {
        discovery().onEntered(at(elsewhere))

        assertTrue(presence.notified.isEmpty())
        assertTrue(store.records.isEmpty())
    }

    /**
     * (b) — the running walk owns the decision.
     *
     * `WalkModeController` loads the record into memory when the walk starts,
     * so a write made behind its back would not be seen by that copy and the
     * story could speak a second time. The passive path keeps quiet instead.
     */
    @Test
    fun `nothing is notified while a walk is running`() = runBlocking {
        discovery(walkRunning = true).onEntered(at(crossing))

        assertTrue(presence.notified.isEmpty())
        assertTrue(store.records.isEmpty())
    }

    /**
     * Without the background permission nothing registers — and nothing else
     * fails either. `refresh` still takes down the circles of stories that
     * have already spoken, and the trip is read exactly as before.
     */
    @Test
    fun `a refused permission registers nothing and breaks nothing`() = runBlocking {
        geofences.permitted = false
        store.records["story.latin-bridge"] =
            StoryTriggerRecord("story.latin-bridge", triggeredAt = 1L)

        val discovery = discovery()
        discovery.refresh()
        discovery.onEntered(at(crossing))

        assertTrue(geofences.registered.isEmpty())
        // The one that has spoken is taken down; nothing is put up.
        assertTrue(geofences.cancelled.contains("story.latin-bridge"))
        // The receiver never runs without the permission, but if a transition
        // did arrive the decision is the same one, and it still holds.
        assertEquals("story.meeting-of-cultures", presence.notified.single().first)
    }

    @Test
    fun `screen 19 asks the same question the registration asks`() {
        geofences.permitted = false
        assertEquals(false, discovery().canDiscover())
        geofences.permitted = true
        assertEquals(true, discovery().canDiscover())
    }
}

/** The record, in memory, with `recordTriggered`'s first-arrival-wins rule. */
internal class RecordingTriggerStore : StoryTriggerStore {
    val records = mutableMapOf<String, StoryTriggerRecord>()

    override suspend fun all(): Map<String, StoryTriggerRecord> = records.toMap()

    override suspend fun recordTriggered(storyId: String, atMillis: Long) {
        records.putIfAbsent(storyId, StoryTriggerRecord(storyId, triggeredAt = atMillis))
    }

    override suspend fun recordPlayed(storyId: String, atMillis: Long) {
        records[storyId] = records[storyId]?.copy(playedAt = atMillis)
            ?: StoryTriggerRecord(storyId, triggeredAt = atMillis, playedAt = atMillis)
    }
}

/** What was asked of Play Services, without Play Services. */
private class RecordingGeofences : StoryGeofences {
    var permitted = true
    val registered = mutableListOf<Story>()
    val cancelled = mutableListOf<String>()

    override fun canRegister(): Boolean = permitted

    override fun register(stories: List<Story>) {
        registered += stories
    }

    override fun cancel(storyIds: List<String>) {
        cancelled += storyIds
    }

    /** The radius production would register a story at. */
    fun radiusOf(storyId: String): Float? = registered.firstOrNull { it.id == storyId }
        ?.trigger
        ?.let(::registrationRadiusMeters)
}

private class RecordingPresence : StoryDiscoveryPresence {
    val notified = mutableListOf<Triple<String, String, String>>()

    override fun notifyStoryOutsideWalk(storyId: String, title: String, hook: String) {
        notified += Triple(storyId, title, hook)
    }
}
