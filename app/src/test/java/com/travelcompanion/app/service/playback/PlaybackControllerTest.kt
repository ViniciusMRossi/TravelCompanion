package com.travelcompanion.app.service.playback

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Playback coordination: resolving packaged audio, surviving the screen,
 * restoring a position, and failing in a way the traveller can understand.
 *
 * `Dispatchers.Unconfined` runs the controller's coroutines eagerly, so these
 * assertions need no virtual clock.
 */
class PlaybackControllerTest {

    private val engine = FakeAudioEngine()
    private val positions = InMemoryPlaybackPositionStore()
    private val controller = PlaybackController(
        engine = engine,
        positions = positions,
        scope = CoroutineScope(Dispatchers.Unconfined),
    )

    private val content = packagedContent(exists = { true })

    private fun playableRequest(): AudioGuideRequest =
        audioGuideRequest(content, "ag.bascarsija", subtitle = "Baščaršija")!!

    @Test
    fun aPackagedAudioGuideResolvesToALocalAssetAndIsPrepared() {
        val request = playableRequest()

        controller.playAudioGuide(request)

        assertEquals("ag.bascarsija", engine.preparedMediaId)
        assertEquals("asset:///trip/audio/attractions/bascarsija.prototype.wav", engine.preparedUri)
        assertEquals("ag.bascarsija", controller.state.value.mediaId)
    }

    /**
     * The transport's state is what was asked for, not what has been confirmed.
     *
     * `seekTo` always worked this way; `play` and `pause` did not, and left
     * every reader depending on when the engine's listener answers. Media3's
     * `MediaController` happens to answer from inside the call, so this held by
     * accident — and screen 09 publishing `isPlaying` to the group is a reader
     * that acts in the very next statement (D050).
     */
    @Test
    fun pauseAndPlayReportThemselvesWithoutWaitingForTheEngine() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(durationMs = 720_000L)
        assertTrue(controller.state.value.isPlaying)

        engine.notifiesSynchronously = false

        controller.togglePlayPause()
        assertFalse("a pause has to be visible at once", controller.state.value.isPlaying)

        controller.togglePlayPause()
        assertTrue("and so does resuming", controller.state.value.isPlaying)
    }

    /** A guide that ended still says so: the intent never overwrites it. */
    @Test
    fun askingAFinishedGuideToPauseDoesNotHideThatItEnded() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(durationMs = 720_000L)
        engine.advanceTo(720_000L)
        engine.finish()
        assertEquals(PlaybackState.Status.Ended, controller.state.value.status)

        engine.notifiesSynchronously = false
        controller.pause()

        assertEquals(PlaybackState.Status.Ended, controller.state.value.status)
    }

    @Test
    fun audioIsNeverStreamed() {
        val request = playableRequest() as AudioGuideRequest.Playable

        assertTrue("bundled audio must stay local", request.uri.startsWith("asset:///"))
        assertFalse(request.uri.startsWith("http"))
    }

    @Test
    fun aGuideWithNoPackagedAudioIsNotPlayableAndTouchesNothing() {
        val withoutBinaries = packagedContent(exists = { false })
        val request = audioGuideRequest(withoutBinaries, "ag.bascarsija")!!

        controller.playAudioGuide(request)

        assertTrue(request is AudioGuideRequest.NotPackaged)
        assertNull("nothing may be handed to the player", engine.preparedUri)
        assertEquals("and nothing is claimed about it", PlaybackState.Status.Idle, controller.state.value.status)
    }

    @Test
    fun anUnplayableGuideNeverInterruptsAudioThatIsAlreadyPlaying() {
        // Policy: a content gap in guide B must not stop guide A, and the UI
        // must keep describing what is actually coming out of the speaker.
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(60_000L)
        engine.advanceTo(5_000L)
        controller.refreshProgress()

        val unplayable = audioGuideRequest(packagedContent(exists = { false }), "ag.latin-bridge")!!
        controller.playAudioGuide(unplayable)

        val state = controller.state.value
        assertTrue("the running guide keeps playing", state.isPlaying)
        assertTrue(engine.isPlaying)
        assertEquals("and state still describes it", "ag.bascarsija", state.mediaId)
        assertEquals(1, engine.prepareCount)
    }

    @Test
    fun theNotificationGetsReadableTextRatherThanAnInternalId() {
        controller.playAudioGuide(playableRequest())

        assertEquals("Audioguia de Baščaršija", engine.preparedTitle)
        assertEquals("Baščaršija", engine.preparedSubtitle)
        assertTrue(
            "the media id must not be what the lock screen shows",
            engine.preparedTitle != engine.preparedMediaId,
        )
    }

    @Test
    fun playPauseUpdatesTheObservableState() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        assertTrue(controller.state.value.isPlaying)
        assertEquals(PlaybackState.Status.Playing, controller.state.value.status)

        controller.togglePlayPause()

        assertFalse(controller.state.value.isPlaying)
        assertEquals(PlaybackState.Status.Paused, controller.state.value.status)

        controller.togglePlayPause()
        assertEquals(PlaybackState.Status.Playing, controller.state.value.status)
    }

    @Test
    fun seekUpdatesPositionAndIsClampedToTheDuration() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(60_000L)

        controller.seekTo(20_000L)
        assertEquals(20_000L, controller.state.value.positionMs)
        assertEquals(20_000L, engine.positionMs)

        controller.seekBy(15_000L)
        assertEquals(35_000L, controller.state.value.positionMs)

        controller.seekBy(-100_000L)
        assertEquals("never before the start", 0L, controller.state.value.positionMs)

        controller.seekTo(999_000L)
        assertEquals("never past the end", 60_000L, controller.state.value.positionMs)
    }

    @Test
    fun durationAndPositionAreObservable() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(120_000L)
        engine.advanceTo(30_000L)

        controller.refreshProgress()

        val state = controller.state.value
        assertEquals(120_000L, state.durationMs)
        assertEquals(30_000L, state.positionMs)
        assertEquals(0.25f, state.progress, 0.001f)
    }

    @Test
    fun pausingPersistsThePositionAndPlayingAgainRestoresIt() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(120_000L)
        engine.advanceTo(42_000L)
        controller.refreshProgress()

        controller.pause()
        assertEquals(42_000L, positions.peek("ag.bascarsija"))

        // A fresh controller, as after the process was recreated.
        val restored = PlaybackController(
            engine = FakeAudioEngine(),
            positions = positions,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        restored.playAudioGuide(playableRequest())

        assertEquals(42_000L, restored.state.value.positionMs)
    }

    @Test
    fun aFinishedGuideStartsFromTheBeginningNextTime() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(60_000L)
        engine.advanceTo(60_000L)
        controller.refreshProgress()

        engine.finish()

        assertEquals(PlaybackState.Status.Ended, controller.state.value.status)
        assertNull("a completed guide keeps no resume point", positions.peek("ag.bascarsija"))

        // The "next time": tapping again must actually play, from the top.
        engine.commands.clear()
        controller.playAudioGuide(playableRequest())

        assertEquals(listOf("seekTo(0)", "play"), engine.commands)
        assertEquals(0L, controller.state.value.positionMs)
        assertEquals(0L, engine.positionMs)
        assertEquals(PlaybackState.Status.Playing, controller.state.value.status)
        assertTrue(engine.isPlaying)
    }

    @Test
    fun theToggleOnAFinishedGuideAlsoReplaysIt() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(60_000L)
        engine.advanceTo(60_000L)
        controller.refreshProgress()
        engine.finish()

        engine.commands.clear()
        controller.togglePlayPause()

        assertEquals(listOf("seekTo(0)", "play"), engine.commands)
        assertEquals(PlaybackState.Status.Playing, controller.state.value.status)
    }

    @Test
    fun theCurrentChapterFollowsThePosition() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        assertEquals(3, controller.state.value.chapters.size)
        assertEquals(0, controller.state.value.currentChapterIndex)

        engine.advanceTo(250_000L)
        controller.refreshProgress()
        assertEquals("crossing 240s enters chapter 2", 1, controller.state.value.currentChapterIndex)

        engine.advanceTo(500_000L)
        controller.refreshProgress()
        assertEquals(2, controller.state.value.currentChapterIndex)
        assertEquals("Protótipo · trecho 3", controller.state.value.currentChapter?.title)
    }

    @Test
    fun aChapterCanBeSelectedAndSeeksToItsStart() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        controller.seekToChapter(2)

        assertEquals(480_000L, controller.state.value.positionMs)
        assertEquals(480_000L, engine.positionMs)
        assertEquals(2, controller.state.value.currentChapterIndex)
    }

    @Test
    fun chapterSkippingStaysInsideTheGuide() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        controller.skipToNextChapter()
        assertEquals(1, controller.state.value.currentChapterIndex)

        controller.skipToPreviousChapter()
        assertEquals(0, controller.state.value.currentChapterIndex)

        controller.skipToPreviousChapter()
        assertEquals("there is nothing before the first chapter", 0, controller.state.value.currentChapterIndex)

        controller.seekToChapter(99)
        assertEquals("nor after the last", 0, controller.state.value.currentChapterIndex)
    }

    @Test
    fun theCompactPlayerNamesTheChapterItIsIn() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)
        engine.advanceTo(250_000L)
        controller.refreshProgress()

        assertEquals("Baščaršija · capítulo 2", controller.state.value.compactSubtitle())
    }

    @Test
    fun aPlayerErrorFailsLoudlyInStateButDoesNotThrow() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(60_000L)

        engine.fail()

        assertEquals(PlaybackState.Status.Failed, controller.state.value.status)
        assertEquals(PlaybackFailure.PlaybackFailed, controller.state.value.failure)
    }

    @Test
    fun askingForTheSameGuideAgainTogglesInsteadOfRestarting() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(60_000L)
        engine.advanceTo(10_000L)
        controller.refreshProgress()

        controller.playAudioGuide(playableRequest())

        assertFalse("the second tap pauses", controller.state.value.isPlaying)
        assertEquals("the guide is never re-prepared", 1, engine.prepareCount)
        assertEquals("so the place is kept", 10_000L, controller.state.value.positionMs)
    }

    @Test
    fun aPauseSignalAfterTheGuideEndsDoesNotDowngradeEnded() {
        // Media3 order A: STATE_ENDED, then isPlaying=false.
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        engine.finish()
        engine.emitPlayingChanged(false)

        assertEquals(PlaybackState.Status.Ended, controller.state.value.status)
    }

    @Test
    fun endingAfterAPauseSignalStillReachesEnded() {
        // Media3 order B: isPlaying=false, then STATE_ENDED.
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        engine.emitPlayingChanged(false)
        assertEquals(PlaybackState.Status.Paused, controller.state.value.status)

        engine.emitEnded()

        assertEquals(PlaybackState.Status.Ended, controller.state.value.status)
    }

    @Test
    fun aPauseSignalAfterAFailureDoesNotDowngradeFailed() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        engine.fail()
        engine.emitPlayingChanged(false)

        assertEquals(PlaybackState.Status.Failed, controller.state.value.status)
        assertEquals(PlaybackFailure.PlaybackFailed, controller.state.value.failure)
    }

    @Test
    fun failingAfterAPauseSignalStillReachesFailed() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)

        engine.emitPlayingChanged(false)
        engine.fail()

        assertEquals(PlaybackState.Status.Failed, controller.state.value.status)
    }

    @Test
    fun replayStillWorksAfterEndArrivesAlongsideAPauseSignal() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)
        engine.advanceTo(720_000L)
        controller.refreshProgress()

        engine.finish()
        engine.emitPlayingChanged(false)
        assertEquals(PlaybackState.Status.Ended, controller.state.value.status)

        engine.commands.clear()
        controller.playAudioGuide(playableRequest())

        assertEquals(listOf("seekTo(0)", "play"), engine.commands)
        assertEquals(PlaybackState.Status.Playing, controller.state.value.status)
        assertEquals(0L, controller.state.value.positionMs)
    }

    @Test
    fun aGuideThatEndedKeepsNoResumePointEvenWhenPauseArrivesAfterwards() {
        controller.playAudioGuide(playableRequest())
        engine.becomeReady(720_000L)
        engine.advanceTo(720_000L)
        controller.refreshProgress()

        engine.finish()
        engine.emitPlayingChanged(false)

        assertNull(
            "a pause signal after the end must not write the end back as a resume point",
            positions.peek("ag.bascarsija"),
        )
    }

    @Test
    fun nothingInTheObservableApiExposesMedia3() {
        val surface = buildList {
            addAll(PlaybackController::class.java.methods.map { it.returnType.name })
            addAll(PlaybackController::class.java.methods.flatMap { m -> m.parameterTypes.map { it.name } })
            addAll(PlaybackState::class.java.methods.map { it.returnType.name })
            addAll(AudioEngine::class.java.methods.flatMap { m -> m.parameterTypes.map { it.name } })
        }

        val leaked = surface.filter { it.startsWith("androidx.media3") }
        assertTrue("playback API must not leak player types: $leaked", leaked.isEmpty())
        assertNotNull(controller.state)
    }
}
