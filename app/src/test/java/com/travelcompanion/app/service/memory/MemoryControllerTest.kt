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
    private val player = FakePlayer()
    private val repository = FakeMemories()
    private var nowMs = 0L

    private val content = packagedContent(exists = { true })

    private val controller by lazy {
        MemoryController(
            recorder = recorder,
            memories = repository,
            playback = playback,
            player = player,
            filesDir = { temp.root },
            scope = CoroutineScope(Dispatchers.Unconfined),
            now = { nowMs },
            epochNow = { 1_700_000_000_000L },
            newId = { "m1" },
        )
    }

    private val who = MemoryAttribution("vinicius", "Vinícius", "Sarajevo", "Baščaršija")

    /** One finished recording, which is what every playback case needs. */
    private fun record(): Memory {
        controller.start(who)
        nowMs += 5_000L
        controller.finish()
        return repository.rows.last()
    }

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

    // -- playing a memory back --------------------------------------------

    /**
     * Same rule as recording, one caller further: a memory takes the guide's
     * place while it plays and gives it back when it stops (D081).
     */
    @Test
    fun `playing a memory pauses the audioguide and stopping gives it back`() {
        playGuide()
        val memory = record()

        controller.playMemory(memory)
        assertFalse("the guide gives way to the memory", playback.state.value.isPlaying)
        assertEquals("m1", controller.playing.value.memoryId)

        controller.stopMemory()
        assertTrue("and comes back when the memory stops", playback.state.value.isPlaying)
    }

    /** The invariant this file already holds for recording, for playback. */
    @Test
    fun `a guide that was already paused stays paused while a memory plays`() {
        playGuide()
        playback.pause()
        val memory = record()

        controller.playMemory(memory)
        controller.stopMemory()

        assertFalse("playing a memory is not a reason to start a guide", playback.state.value.isPlaying)
    }

    @Test
    fun `a memory that ends gives the guide back by itself`() {
        playGuide()
        val memory = record()
        controller.playMemory(memory)

        player.finish()

        assertEquals("back to the start, icon on play", null, controller.playing.value.memoryId)
        assertTrue(playback.state.value.isPlaying)
    }

    /** Only one memory plays: starting one stops whatever was running. */
    @Test
    fun `starting a memory stops the one that was playing`() {
        val first = record()
        val second = first.copy(id = "m2")
        controller.fileFor(second.id).writeText("fake audio")
        controller.playMemory(first)
        assertTrue(player.isPlaying)

        controller.playMemory(second)

        assertEquals("m2", controller.playing.value.memoryId)
        assertEquals(1, player.stops)
    }

    /** A file that will not play says so instead of pretending to run. */
    @Test
    fun `a memory whose file is gone reports itself unplayable`() {
        val memory = record()
        controller.fileFor(memory.id).delete()

        controller.playMemory(memory)

        assertEquals("m1", controller.playing.value.unplayableMemoryId)
        assertFalse(controller.playing.value.isPlaying)
    }

    /** Recording is not a thing to do over a memory that is playing. */
    @Test
    fun `starting a recording stops a memory that was playing`() {
        val memory = record()
        controller.playMemory(memory)

        controller.start(who)

        assertEquals(null, controller.playing.value.memoryId)
    }

    // -- deleting ---------------------------------------------------------

    /**
     * The dialog says the recording leaves the phone, so both halves go: the
     * row and the file. Nothing tries to recall what was already shared.
     */
    @Test
    fun `deleting takes the row and the file`() {
        val memory = record()
        assertTrue(controller.fileFor("m1").exists())

        controller.deleteMemory(memory)

        assertTrue("the row is gone", repository.rows.isEmpty())
        assertFalse("and so is the audio", controller.fileFor("m1").exists())
    }

    @Test
    fun `deleting the memory that is playing stops it first`() {
        playGuide()
        val memory = record()
        controller.playMemory(memory)

        controller.deleteMemory(memory)

        assertEquals(null, controller.playing.value.memoryId)
        assertTrue("and the guide comes back", playback.state.value.isPlaying)
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

    private class FakePlayer : MemoryAudioPlayer {
        var stops = 0
        private var completion: (() -> Unit)? = null
        private var running = false

        override fun play(file: File, onCompleted: () -> Unit): Boolean {
            if (!file.exists()) return false
            completion = onCompleted
            running = true
            return true
        }

        override fun pause() {
            running = false
        }

        override fun resume(): Boolean {
            if (completion == null) return false
            running = true
            return true
        }

        override fun stop() {
            if (completion != null) stops += 1
            completion = null
            running = false
        }

        /** The file reaching its end, which the real player reports. */
        fun finish() {
            val done = completion
            running = false
            done?.invoke()
        }

        override val isPlaying: Boolean get() = running
        override val positionMs: Long get() = 0L
        override val durationMs: Long get() = 48_000L
    }

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

        override suspend fun delete(id: String) {
            rows.removeAll { it.id == id }
            flow.value = rows.toList()
        }
    }
}
