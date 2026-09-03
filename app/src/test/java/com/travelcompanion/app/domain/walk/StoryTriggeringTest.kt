package com.travelcompanion.app.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §20, driven with synthetic positions.
 *
 * Idempotence and `notifyOncePerTrip` are the real risk: a story that fires
 * twice interrupts a walk the traveller is not looking at, and the phone is in
 * a pocket when it happens.
 */
class StoryTriggeringTest {

    private val meeting = story("story.meeting-of-cultures", MEETING_POINT, radiusMeters = 120.0)
    private val bridge = story("story.latin-bridge", BRIDGE_POINT, radiusMeters = 100.0)
    private val stories = listOf(meeting, bridge)

    private fun routeContext(next: String?, automatic: Boolean = true) = WalkRouteContext(
        walkId = "walk.sarajevo.historical",
        routeStoryIds = listOf(meeting.id, bridge.id),
        nextStoryId = next,
        automaticStories = automatic,
    )

    private fun triggeredAt(vararg ids: String): Map<String, StoryTriggerRecord> =
        ids.associateWith { StoryTriggerRecord(it, triggeredAt = 1L) }

    @Test
    fun `a position far from everything decides nothing`() {
        val decision = decideStoryTrigger(at(AWAY_POINT), stories, emptyMap(), routeContext(meeting.id))
        assertEquals(StoryTriggerDecision.Nothing, decision)
    }

    @Test
    fun `arriving on a walk plays the story automatically`() {
        val decision = decideStoryTrigger(at(MEETING_POINT), stories, emptyMap(), routeContext(meeting.id))
        assertEquals(StoryTriggerDecision.PlayInWalk(meeting), decision)
    }

    @Test
    fun `notifyOncePerTrip makes the same position idempotent`() {
        val context = routeContext(meeting.id)
        val first = decideStoryTrigger(at(MEETING_POINT), stories, emptyMap(), context)
        assertTrue(first is StoryTriggerDecision.PlayInWalk)

        // Exactly what a 1 Hz location stream does: the same fix again.
        val recorded = triggeredAt(meeting.id)
        repeat(3) {
            assertEquals(
                StoryTriggerDecision.Nothing,
                decideStoryTrigger(at(MEETING_POINT), stories, recorded, context),
            )
        }
    }

    @Test
    fun `a story that opted out of once-per-trip may fire again`() {
        val repeatable = story("story.repeatable", MEETING_POINT, notifyOncePerTrip = false)
        val decision = decideStoryTrigger(
            at(MEETING_POINT),
            listOf(repeatable),
            triggeredAt(repeatable.id),
            routeContext(repeatable.id).copy(routeStoryIds = listOf(repeatable.id)),
        )
        assertEquals(StoryTriggerDecision.PlayInWalk(repeatable), decision)
    }

    @Test
    fun `the walk route wins over standing inside another circle`() {
        // Both circles cover this position; the walk is heading for the bridge.
        val overlapping = story("story.overlapping", MEETING_POINT, radiusMeters = 500.0)
        val all = listOf(overlapping, meeting, bridge)
        val context = WalkRouteContext(
            walkId = "walk.sarajevo.historical",
            routeStoryIds = listOf(meeting.id, bridge.id, overlapping.id),
            nextStoryId = bridge.id,
            automaticStories = true,
        )
        val decision = decideStoryTrigger(at(BRIDGE_POINT), all, emptyMap(), context)
        assertEquals(StoryTriggerDecision.PlayInWalk(bridge), decision)
    }

    @Test
    fun `a story from another walk cannot interrupt this one`() {
        val foreign = story("story.other-walk", MEETING_POINT, walkId = "walk.other")
        val context = routeContext(bridge.id)
        val decision = decideStoryTrigger(at(MEETING_POINT), listOf(foreign), emptyMap(), context)
        assertEquals(StoryTriggerDecision.Nothing, decision)
    }

    @Test
    fun `outside a walk the answer is a notification, never auto-play`() {
        val decision = decideStoryTrigger(at(MEETING_POINT), stories, emptyMap(), walk = null)
        assertEquals(StoryTriggerDecision.Notify(meeting), decision)
    }

    @Test
    fun `outside a walk the nearest eligible story wins`() {
        // A position inside both circles, closer to the bridge.
        val wide = story("story.wide", MEETING_POINT, radiusMeters = 1000.0)
        val decision = decideStoryTrigger(at(BRIDGE_POINT), listOf(wide, bridge), emptyMap(), walk = null)
        assertEquals(StoryTriggerDecision.Notify(bridge), decision)
    }

    @Test
    fun `a story that does not auto-play is offered, not forced`() {
        val quiet = story("story.quiet", MEETING_POINT, autoPlayInWalk = false)
        val context = routeContext(quiet.id).copy(routeStoryIds = listOf(quiet.id))
        val decision = decideStoryTrigger(at(MEETING_POINT), listOf(quiet), emptyMap(), context)
        assertEquals(StoryTriggerDecision.Notify(quiet), decision)
    }

    @Test
    fun `turning automatic stories off downgrades auto-play to a notification`() {
        val decision = decideStoryTrigger(
            at(MEETING_POINT),
            stories,
            emptyMap(),
            routeContext(meeting.id, automatic = false),
        )
        assertEquals(StoryTriggerDecision.Notify(meeting), decision)
    }

    @Test
    fun `a fix wider than the trigger radius decides nothing yet`() {
        val decision = decideStoryTrigger(
            at(MEETING_POINT, accuracyMeters = 400f),
            stories,
            emptyMap(),
            routeContext(meeting.id),
        )
        assertEquals(StoryTriggerDecision.Nothing, decision)
    }

    @Test
    fun `a fix with no accuracy reported is still allowed to decide`() {
        val decision = decideStoryTrigger(
            at(MEETING_POINT, accuracyMeters = null),
            stories,
            emptyMap(),
            routeContext(meeting.id),
        )
        assertEquals(StoryTriggerDecision.PlayInWalk(meeting), decision)
    }
}
