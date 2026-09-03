package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.service.playback.FakeAudioEngine
import com.travelcompanion.app.service.playback.InMemoryPlaybackPositionStore
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Screen 05's audioguide binding.
 *
 * The point of these is the lifetime: the player belongs to the application,
 * so walking away from the screen must not stop the audio.
 */
class AttractionPlaybackTest {

    private val content = packagedContent(exists = { true })
    private val engine = FakeAudioEngine()
    private val controller = PlaybackController(
        engine = engine,
        positions = InMemoryPlaybackPositionStore(),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    private fun viewModel() = AttractionViewModel(
        content = content,
        attractionId = "bascarsija",
        playback = controller,
        today = LocalDate.parse(content.days.first().date),
    )

    @Test
    fun theScreenPlaysTheAttractionsOwnAudioGuide() {
        val viewModel = viewModel()

        viewModel.onPlayAudioGuide()

        assertEquals("ag.bascarsija", viewModel.audioGuideMediaId)
        assertEquals("ag.bascarsija", engine.preparedMediaId)
        assertTrue(controller.state.value.isPlaying)
    }

    @Test
    fun theScreenNeverTearsDownPlaybackWhenItGoesAway() {
        // The only way a screen could stop the audioguide is by reaching for
        // the controller as it is destroyed, so it must not override onCleared.
        val overridesOnCleared = runCatching {
            AttractionViewModel::class.java.getDeclaredMethod("onCleared")
        }.isSuccess

        assertFalse(
            "a screen-scoped ViewModel must not tear down application-scoped playback",
            overridesOnCleared,
        )
    }

    @Test
    fun playbackKeepsRunningAfterTheScreenIsDiscarded() {
        viewModel().onPlayAudioGuide()
        engine.becomeReady(720_000L)

        // Navigating away: nothing else holds the screen's ViewModel.
        System.gc()

        assertTrue("audio must survive navigation", controller.state.value.isPlaying)
        assertTrue(engine.isPlaying)
        assertEquals("the session is never torn down by a screen", 0, engine.stopCount)
    }

    @Test
    fun aSecondVisitObservesTheStillRunningPlayback() {
        val first = viewModel()
        first.onPlayAudioGuide()
        engine.becomeReady(720_000L)
        engine.advanceTo(30_000L)
        controller.refreshProgress()

        val second = viewModel()

        val state: PlaybackState = second.playbackState.value
        assertTrue(state.isFor(second.audioGuideMediaId))
        assertTrue(state.isPlaying)
        assertEquals(30_000L, state.positionMs)
    }

    @Test
    fun skipControlsMoveThePosition() {
        val viewModel = viewModel()
        viewModel.onPlayAudioGuide()
        engine.becomeReady(720_000L)
        engine.advanceTo(60_000L)
        controller.refreshProgress()

        viewModel.onSkipForward()
        assertEquals(75_000L, controller.state.value.positionMs)

        viewModel.onSkipBack()
        assertEquals(60_000L, controller.state.value.positionMs)
    }

    @Test
    fun anAttractionWithoutAnAudioGuideOffersNoPlayback() {
        val viewModel = AttractionViewModel(
            content = content,
            attractionId = "does-not-exist",
            playback = controller,
            today = LocalDate.parse(content.days.first().date),
        )

        viewModel.onPlayAudioGuide()

        assertNull(viewModel.state)
        assertNull("nothing was handed to the player", engine.preparedMediaId)
    }
}
