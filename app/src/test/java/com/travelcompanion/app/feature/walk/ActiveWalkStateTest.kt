package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.domain.walk.LocationQuality
import com.travelcompanion.app.domain.walk.WalkModeState
import com.travelcompanion.app.domain.walk.WalkPhase
import com.travelcompanion.app.domain.walk.WalkStopState
import com.travelcompanion.app.service.playback.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What screen 07's header is allowed to say.
 *
 * The screen is read at a glance with the phone coming out of a pocket, so the
 * story it names has to be the story in the traveller's ear (D053).
 */
class ActiveWalkStateTest {

    private fun walkState(
        stops: List<WalkStopState>,
        currentStopIndex: Int?,
    ) = WalkModeState(
        phase = WalkPhase.Active,
        walkId = "walk.sarajevo.historical",
        walkTitle = "Caminhada Histórica de Sarajevo",
        stops = stops,
        currentStopIndex = currentStopIndex,
        locationQuality = LocationQuality.Active,
    )

    private fun playing(mediaId: String?) = PlaybackState(
        mediaId = mediaId,
        status = if (mediaId == null) PlaybackState.Status.Idle else PlaybackState.Status.Playing,
    )

    private val bascarsija = WalkStopState(
        storyId = "story.bascarsija",
        title = "Baščaršija",
        instructionToNext = "Siga pela Ferhadija.",
        visited = true,
        audioGuideId = "ag.bascarsija",
    )

    /** The guide this build does not carry, which is how the defect appeared. */
    private val latinBridge = WalkStopState(
        storyId = "story.latin-bridge",
        title = "Latin Bridge",
        instructionToNext = null,
        audioGuideId = "ag.latin-bridge",
    )

    @Test
    fun `the header names the story whose guide is playing`() {
        val state = buildActiveWalkState(
            walkState = walkState(listOf(bascarsija, latinBridge), currentStopIndex = 0),
            playback = playing("ag.bascarsija"),
            walk = null,
        )

        assertEquals("Baščaršija", state.storyTitle)
    }

    /**
     * Arriving at a story whose audio is not packaged left the header saying
     * "TOCANDO AGORA · Latin Bridge" over the guide still running from the stop
     * before. Watched on the device; the header must not claim it.
     */
    @Test
    fun `a story whose guide is not packaged is not what the header names`() {
        val state = buildActiveWalkState(
            walkState = walkState(listOf(bascarsija, latinBridge), currentStopIndex = 1),
            // Nothing loaded it, so the player is still on the previous guide.
            playback = playing("ag.bascarsija"),
            walk = null,
        )

        assertEquals("Caminhada Histórica de Sarajevo", state.storyTitle)
    }

    /** A story with no audio at all is not named either. */
    @Test
    fun `a story with no guide is not what the header names`() {
        val silent = latinBridge.copy(audioGuideId = null)
        val state = buildActiveWalkState(
            walkState = walkState(listOf(bascarsija, silent), currentStopIndex = 1),
            playback = playing("ag.bascarsija"),
            walk = null,
        )

        assertEquals("Caminhada Histórica de Sarajevo", state.storyTitle)
    }

    /**
     * Where to walk is true whether or not the story could be narrated, so the
     * instruction follows the arrival rather than the audio.
     */
    @Test
    fun `the walking instruction still belongs to the stop that was reached`() {
        val state = buildActiveWalkState(
            walkState = walkState(listOf(bascarsija, latinBridge), currentStopIndex = 0),
            playback = playing("ag.latin-bridge"),
            walk = null,
        )

        assertEquals("Siga pela Ferhadija.", state.instruction)
    }
}
