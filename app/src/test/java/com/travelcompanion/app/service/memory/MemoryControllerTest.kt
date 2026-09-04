package com.travelcompanion.app.service.memory

import com.travelcompanion.app.data.memory.Memory
import com.travelcompanion.app.data.memory.MemoryRepository
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.memory.MemoryAttribution
import com.travelcompanion.app.domain.memory.MemoryFailure
import com.travelcompanion.app.domain.memory.MemoryPhase
import com.travelcompanion.app.service.playback.FakeAudioEngine
import com.travelcompanion.app.service.playback.InMemoryPlaybackPositionStore
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.audioGuideRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Recording against a fake microphone.
 *
 * The rule this class exists for is brief §22's: a recording that has been
 * finished is never lost. Everything after `stop()` is bookkeeping, and no
 * failure in the bookkeeping may throw the audio away.
 */
class MemoryControllerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val engine = FakeAudioEngine()
    private val playback = PlaybackController(
        engine = engine,
        positions = InMemoryPlaybackPositionStore(),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
    private val recorder = FakeRecorder()
    private val repository = FakeMemories()
    private var nowMs = 0L

    private val content = packagedContent(exists = { true })

    private val controller by lazy {
        MemoryController(
            recorder = recorder,
            memories = repository,
            playback = playback,
            filesDir = { temp.root },
            scope = CoroutineScope(Dispatchers.Unconfined),
            now = { nowMs },
            epochNow = { 1_700_000_000_000L },
            newId = { "m1" },
        )
    }

    private val who = MemoryAttribution("vinicius", "Vinícius", "Sarajevo", "Baščaršija")

    private fun playGuide() {
        playback.playAudioGuide(audioGuideRequest(content, "ag.bascarsija")!!)
        engine.becomeReady(durationMs = 720_000L)
    }

    // -- the guide gives way, and comes back ------------------------------

    /**
     * The microphone would otherwise record the narration on a speaker, or
     * talk over the traveller in headphones. The guide comes back afterwards
     * because it was this that stopped it (D058).
     */
    @Test
    fun `recording pauses the audioguide and finishing gives it back`() {
        playGuide()
        assertTrue(playback.state.value.isPlaying)

        controller.start(who)
        assertFalse("the guide gives way to the microphone", playback.state.value.isPlaying)

        nowMs = 4_000L
        controller.finish()
        assertTrue("and comes back when the recording ends", playback.state.value.isPlaying)
    }

    @Test
    fun `cancelling gives the audioguide back too`() {
        playGuide()
        controller.start(who)
        controller.cancel()
        assertTrue(playback.state.value.isPlaying)
    }

    /** A guide the traveller had already paused is not started by recording. */
    @Test
    fun `a guide that was already paused stays paused`() {
        playGuide()
        playback.pause()
        assertFalse(playback.state.value.isPlaying)

        controller.start(who)
        controller.finish()

        assertFalse("recording is not a reason to start playing", playback.state.value.isPlaying)
    }

    // -- never lose a finished recording ----------------------------------

    @Test
    fun `finishing writes the row and keeps the file`() {
        controller.start(who)
        nowMs = 6_000L
        controller.finish()

        assertEquals(MemoryPhase.Saved, controller.state.value.phase)
        val saved = repository.rows.single()
        assertEquals("m1", saved.id)
        assertEquals(6_000L, saved.durationMs)
        assertEquals("Baščaršija", saved.placeName)
        assertTrue("the audio must still be on disk", controller.fileFor("m1").exists())
    }

    /**
     * The case the invariant is about: the audio exists and the row does not.
     * The recording is not thrown away and the screen is told something it can
     * repeat to a person.
     */
    @Test
    fun `a row that cannot be written does not cost the recording`() {
        repository.failOnSave = true

        controller.start(who)
        nowMs = 9_000L
        controller.finish()

        assertEquals(MemoryPhase.Failed, controller.state.value.phase)
        assertEquals(MemoryFailure.SavedFileOnly, controller.state.value.failure)
        assertEquals("the duration survives", 9_000L, controller.state.value.bankedMs)
        assertTrue("the audio must survive a failed write", controller.fileFor("m1").exists())
    }

    @Test
    fun `a recorder that will not start reports it and leaves the guide playing`() {
        playGuide()
        recorder.startSucceeds = false

        controller.start(who)

        assertEquals(MemoryPhase.Failed, controller.state.value.phase)
        assertEquals(MemoryFailure.RecorderFailed, controller.state.value.failure)
        assertTrue("nothing was recorded, so nothing was taken away", playback.state.value.isPlaying)
    }

    @Test
    fun `a recorder that fails to finalize does not claim a memory`() {
        controller.start(who)
        recorder.stopSucceeds = false
        nowMs = 3_000L
        controller.finish()

        assertEquals(MemoryPhase.Failed, controller.state.value.phase)
        assertEquals(MemoryFailure.RecorderFailed, controller.state.value.failure)
        assertTrue("no row for a recording that does not exist", repository.rows.isEmpty())
    }

    @Test
    fun `cancelling throws the audio away, and only cancelling does`() {
        controller.start(who)
        assertTrue(controller.fileFor("m1").exists())

        controller.cancel()

        assertEquals(MemoryPhase.Idle, controller.state.value.phase)
        assertFalse(controller.fileFor("m1").exists())
        assertTrue(repository.rows.isEmpty())
    }

    /** Brief §22's layout, so a memory can be found again without the app. */
    @Test
    fun `audio lands in files memories as an m4a named by its id`() {
        controller.start(who)
        val file = controller.fileFor("m1")
        assertEquals("m1.m4a", file.name)
        assertEquals("memories", file.parentFile?.name)
    }

    // ---------------------------------------------------------------------

    private class FakeRecorder : AudioRecorder {
        var startSucceeds = true
        var stopSucceeds = true
        private var target: File? = null

        override fun start(output: File): Boolean {
            if (!startSucceeds) return false
            output.parentFile?.mkdirs()
            output.writeText("fake audio")
            target = output
            return true
        }

        override fun pause() = Unit
        override fun resume() = Unit

        override fun stop(): Boolean {
            val file = target
            target = null
            if (!stopSucceeds) {
                file?.delete()
                return false
            }
            return file != null
        }

        override fun cancel() {
            target?.delete()
            target = null
        }

        override fun level(): Float = 0.5f
    }

    private class FakeMemories : MemoryRepository {
        val rows = mutableListOf<Memory>()
        var failOnSave = false
        private val flow = MutableStateFlow<List<Memory>>(emptyList())

        override fun memories(): Flow<List<Memory>> = flow

        override suspend fun save(memory: Memory) {
            if (failOnSave) throw IllegalStateException("disk full")
            rows += memory
            flow.value = rows.toList()
        }
    }
}
