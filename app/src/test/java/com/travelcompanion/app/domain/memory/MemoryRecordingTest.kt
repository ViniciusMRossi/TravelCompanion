package com.travelcompanion.app.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recording machine, driven with a fake clock and no microphone.
 *
 * Pausing is what makes this worth a value rather than a start timestamp: a
 * single start time cannot survive it, and the approved flow has a Pausar
 * button.
 */
class MemoryRecordingTest {

    private val who = MemoryAttribution(
        participantId = "vinicius",
        participantName = "Vinícius",
        cityName = "Sarajevo",
        placeName = "Baščaršija",
    )

    private fun started(at: Long = 1_000L) =
        MemoryRecordingState().startRecording("m1", nowMs = at, epochMs = 1_700_000_000_000L, attribution = who)

    @Test
    fun `stopped to recording to paused to finished`() {
        var state = started(at = 1_000L)
        assertEquals(MemoryPhase.Recording, state.phase)
        assertEquals(4_000L, state.elapsedMs(nowMs = 5_000L))

        state = state.pauseRecording(nowMs = 5_000L)
        assertEquals(MemoryPhase.Paused, state.phase)
        // A paused recording does not age.
        assertEquals(4_000L, state.elapsedMs(nowMs = 60_000L))

        state = state.resumeRecording(nowMs = 60_000L)
        assertEquals(MemoryPhase.Recording, state.phase)
        assertEquals(6_000L, state.elapsedMs(nowMs = 62_000L))

        state = state.finishRecording(nowMs = 62_000L)
        assertEquals(MemoryPhase.Saved, state.phase)
        assertEquals("the banked time is what was recorded", 6_000L, state.bankedMs)
    }

    @Test
    fun `a second tap cannot restart a recording that is running`() {
        val state = started()
        val again = state.startRecording("m2", nowMs = 9_000L, epochMs = 1L, attribution = who)
        assertEquals("m1", again.memoryId)
    }

    @Test
    fun `pause and resume only move in the direction they mean`() {
        val recording = started()
        assertEquals(recording, recording.resumeRecording(nowMs = 2_000L))

        val paused = recording.pauseRecording(nowMs = 2_000L)
        assertEquals(paused, paused.pauseRecording(nowMs = 3_000L))
    }

    /** "Concluir" from a pause is the same finish: the audio still exists. */
    @Test
    fun `finishing from paused keeps what was recorded`() {
        val state = started(at = 0L).pauseRecording(nowMs = 3_000L).finishRecording(nowMs = 90_000L)
        assertEquals(MemoryPhase.Saved, state.phase)
        assertEquals(3_000L, state.bankedMs)
    }

    @Test
    fun `cancel is the only path that gives up the audio`() {
        val state = started().cancelRecording()
        assertEquals(MemoryPhase.Idle, state.phase)
        assertNull(state.memoryId)
        assertEquals(0L, state.bankedMs)
    }

    /**
     * The failure that must never read as "nothing happened": the audio is on
     * disk and only its row is missing (brief §22).
     */
    @Test
    fun `a row that could not be written keeps the recording`() {
        val done = started(at = 0L).finishRecording(nowMs = 12_000L)
        val failed = done.failRecording(MemoryFailure.SavedFileOnly, nowMs = 12_000L)

        assertEquals(MemoryPhase.Failed, failed.phase)
        assertEquals(MemoryFailure.SavedFileOnly, failed.failure)
        assertEquals("the duration survives, because the file does", 12_000L, failed.bankedMs)
        assertEquals("m1", failed.memoryId)
    }

    @Test
    fun `a refused microphone gives up the run and nothing else`() {
        val failed = MemoryRecordingState().failRecording(MemoryFailure.NoMicrophonePermission, 0L)
        assertEquals(MemoryPhase.Failed, failed.phase)
        assertNull(failed.memoryId)
        assertTrue(!failed.isActive)
    }

    @Test
    fun `acknowledging returns to rest, and does nothing while recording`() {
        val recording = started()
        assertEquals(recording, recording.acknowledge())
        assertEquals(MemoryPhase.Idle, recording.finishRecording(2_000L).acknowledge().phase)
    }
}
