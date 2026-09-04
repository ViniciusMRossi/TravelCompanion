package com.travelcompanion.app.domain.walk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The §19 machine. Transitions are the real runtime risk here — a walk that
 * ends up Active without ever being Preparing, or a stop marked twice — so
 * they are exercised directly, with no GPS and no Android.
 */
class WalkModeStateTest {

    private val stories = mapOf(
        "s1" to story("s1", MEETING_POINT),
        "s2" to story("s2", BRIDGE_POINT),
    )

    private fun prepared() = prepareWalk(walk("s1", "s2")) { stories[it] }

    @Test
    fun `preparing loads the stops in walk order`() {
        val state = prepared()
        assertEquals(WalkPhase.Preparing, state.phase)
        assertEquals(listOf("s1", "s2"), state.stops.map(WalkStopState::storyId))
        assertEquals("s1", state.nextStop?.storyId)
        assertNull(state.currentStop)
    }

    @Test
    fun `a stop whose story is missing from the package is dropped`() {
        val state = prepareWalk(walk("s1", "ghost")) { stories[it] }
        assertEquals(listOf("s1"), state.stops.map(WalkStopState::storyId))
    }

    @Test
    fun `the full happy path walks Preparing to Completed`() {
        var state = prepared().startWalking()
        assertEquals(WalkPhase.Active, state.phase)
        state = state.pauseWalking()
        assertEquals(WalkPhase.Paused, state.phase)
        state = state.startWalking()
        assertEquals(WalkPhase.Active, state.phase)
        state = state.finishWalking()
        assertEquals(WalkPhase.Finishing, state.phase)
        state = state.completeWalking()
        assertEquals(WalkPhase.Completed, state.phase)
    }

    @Test
    fun `transitions that are not legal leave the state alone`() {
        val idle = WalkModeState()
        assertEquals(idle, idle.startWalking())
        assertEquals(idle, idle.pauseWalking())
        assertEquals(idle, idle.finishWalking())
        assertEquals(idle, idle.completeWalking())

        val preparing = prepared()
        // Not walking yet: cannot pause, and cannot complete without finishing.
        assertEquals(preparing, preparing.pauseWalking())
        assertEquals(preparing, preparing.completeWalking())

        val active = preparing.startWalking()
        // A second tap on "Começar passeio" must not restart anything.
        assertEquals(active, active.startWalking())
    }

    @Test
    fun `arriving marks the stop once and only while active`() {
        val preparing = prepared()
        assertEquals(preparing, preparing.arriveAt("s1"))

        val active = preparing.startWalking()
        val arrived = active.arriveAt("s1")
        assertEquals(0, arrived.currentStopIndex)
        assertTrue(arrived.stops[0].visited)
        assertEquals("s2", arrived.nextStop?.storyId)

        // Same signal again: nothing moves.
        assertEquals(arrived, arrived.arriveAt("s1"))
        // A stop that is not on this walk is ignored.
        assertEquals(arrived, arrived.arriveAt("elsewhere"))
    }

    @Test
    fun `arriving out of order does not rewind the walk`() {
        val state = prepared().startWalking().arriveAt("s1").arriveAt("s2")
        assertEquals(1, state.currentStopIndex)
        assertEquals(2, state.visitedCount)
        assertNull(state.nextStop)

        // A late fix for the first stop arrives after the second: already
        // visited, so it cannot pull the walk backwards.
        val late = state.arriveAt("s1")
        assertEquals(1, late.currentStopIndex)
    }

    @Test
    fun `route context exists only while the walk is actually running`() {
        val preparing = prepared()
        assertNull(preparing.routeContext())
        assertNull(preparing.startWalking().pauseWalking().routeContext())

        val context = preparing.startWalking().routeContext()!!
        assertEquals(listOf("s1", "s2"), context.routeStoryIds)
        assertEquals("s1", context.nextStoryId)
    }

    @Test
    fun `losing location does not stop the walk`() {
        val state = prepared().startWalking().withLocationQuality(LocationQuality.Denied)
        assertEquals(WalkPhase.Active, state.phase)
        assertEquals(LocationQuality.Denied, state.locationQuality)
    }

    /**
     * The sheet is a state of an active walk and nothing else: a stale
     * trigger arriving after the walk ended must not raise a page over the
     * screen that closed it.
     */
    @Test
    fun `a story is only ever offered while the walk is active`() {
        val prepared = prepared()
        assertNull(prepared.offerStory("s2", 40).pending)

        val active = prepared.startWalking()
        assertEquals("s2", active.offerStory("s2", 40).pending?.storyId)
    }

    @Test
    fun `ending the walk takes the sheet with it`() {
        val offered = prepared().startWalking().offerStory("s2", 40)

        val completed = offered.finishWalking().completeWalking()

        assertNull(completed.pending)
        assertEquals(WalkPhase.Completed, completed.phase)
    }

    @Test
    fun `dismissing leaves everything else alone`() {
        val offered = prepared().startWalking().offerStory("s2", 40)

        val cleared = offered.clearPendingStory()

        assertNull(cleared.pending)
        assertEquals(offered.copy(pending = null), cleared)
    }
}
