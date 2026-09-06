package com.travelcompanion.app.feature.story

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.walk.WalkModeState
import com.travelcompanion.app.domain.walk.WalkPhase
import com.travelcompanion.app.domain.walk.WalkStopState
import com.travelcompanion.app.domain.walk.offerStory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen 10 — the story offered by arriving somewhere. */
class StoryTriggerStateTest {

    private val content = packagedContent(exists = { true })

    private fun walking(pendingId: String?, distance: Int? = 40) = WalkModeState(
        phase = WalkPhase.Active,
        walkId = "walk.sarajevo.historical",
        walkTitle = "Caminhada Histórica de Sarajevo",
        stops = listOf(
            WalkStopState("story.meeting-of-cultures", "Onde Oriente e Ocidente se encontram", null),
            WalkStopState("story.latin-bridge", "Latin Bridge", null),
        ),
    ).let { state -> pendingId?.let { state.offerStory(it, distance) } ?: state }

    @Test
    fun `nothing pending is nothing to draw`() {
        assertNull(
            buildStoryTriggerState(content, walking(null)),
        )
    }

    /**
     * The operational facts live on their own line — the sheet keeps them out
     * of the paragraph, and so does the state.
     */
    @Test
    fun `the operational line carries distance, place and audio length`() {
        val state = buildStoryTriggerState(content, walking("story.latin-bridge"))!!

        assertEquals("Latin Bridge", state.title)
        assertEquals("a 40 m · Sarajevo · áudio de 9 min", state.operationalLine)
        assertTrue(state.hasAudio)
        assertFalse(state.body.contains("m ·"))
    }

    /**
     * The line's middle field is a *place*, and never the headline again.
     *
     * It used to read the walk stop's title, and a stop's title is the story's
     * title by construction — so the sheet printed its own h1 a second line
     * later. Both assertions below failed before the fix, and the first one
     * had been written into this file asserting the duplicate.
     */
    @Test
    fun `the place is never the story title repeated`() {
        for (storyId in listOf("story.latin-bridge", "story.meeting-of-cultures")) {
            val state = buildStoryTriggerState(content, walking(storyId))!!
            val place = state.operationalLine.split(" · ").getOrNull(1)

            assertEquals("Sarajevo", place)
            assertFalse(
                "the operational line repeats the title: ${state.operationalLine}",
                state.operationalLine.contains(state.title),
            )
        }
    }

    /** A distance the app does not have is left out, never estimated. */
    @Test
    fun `no fix means no distance on the line`() {
        val state = buildStoryTriggerState(content, walking("story.latin-bridge", distance = null))!!

        assertEquals("Sarajevo · áudio de 9 min", state.operationalLine)
    }

    /** A story with no audio offers no "Ouvir agora". */
    @Test
    fun `a story without audio says so in the state`() {
        val state = buildStoryTriggerState(content, walking("story.meeting-of-cultures"))!!

        assertFalse(state.hasAudio)
        assertTrue(state.fullText.isNotBlank())
    }

    /**
     * The walk above the sheet is screen 07 itself, so this state carries no
     * copy of its progress line — drawing one printed the same sentence twice.
     */
    @Test
    fun `the sheet says how the notice arrived`() {
        val state = buildStoryTriggerState(content, walking("story.latin-bridge"))!!

        assertTrue(state.footnote.contains("no bolso"))
        assertTrue(state.footnote.contains("fones"))
    }
}
