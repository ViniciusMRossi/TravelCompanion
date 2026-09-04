package com.travelcompanion.app.service.memory

import com.travelcompanion.app.data.memory.Memory
import com.travelcompanion.app.data.memory.MemoryRepository
import com.travelcompanion.app.domain.memory.MemoryAttribution
import com.travelcompanion.app.domain.memory.MemoryFailure
import com.travelcompanion.app.domain.memory.MemoryPhase
import com.travelcompanion.app.domain.memory.MemoryRecordingState
import com.travelcompanion.app.domain.memory.acknowledge
import com.travelcompanion.app.domain.memory.cancelRecording
import com.travelcompanion.app.domain.memory.failRecording
import com.travelcompanion.app.domain.memory.finishRecording
import com.travelcompanion.app.domain.memory.pauseRecording
import com.travelcompanion.app.domain.memory.resumeRecording
import com.travelcompanion.app.domain.memory.startRecording
import com.travelcompanion.app.service.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Recording a voice memory, on the application rather than on screen 12.
 *
 * A recording is not screen state: leaving the screen with one running would
 * otherwise throw away something that cannot be recorded again. The decisions
 * are pure functions in `domain/memory`; this drives them with a real
 * microphone, a real clock and a real file, and turns the results into rows.
 *
 * Nothing here touches the group, the packaged trip or the network. A memory
 * is runtime state that stays on this phone (D057).
 */
class MemoryController(
    private val recorder: AudioRecorder,
    private val memories: MemoryRepository,
    private val playback: PlaybackController,
    private val filesDir: () -> File,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val epochNow: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    private val _state = MutableStateFlow(MemoryRecordingState())
    val state: StateFlow<MemoryRecordingState> = _state.asStateFlow()

    private val _level = MutableStateFlow(0f)

    /** 0..1, sampled while recording, for the eight waveform bars. */
    val level: StateFlow<Float> = _level.asStateFlow()

    /** The list under the recorder, straight from Room. */
    fun saved(): Flow<List<Memory>> = memories.memories()

    private var ticker: Job? = null

    /**
     * Whether this controller is the one that paused the audioguide.
     *
     * Only then does it resume it. A guide the traveller had already paused
     * stays paused; recording is not a reason to start something playing.
     */
    private var pausedTheGuide = false

    /**
     * "Registrar memória".
     *
     * The audioguide gives way to the microphone and comes back afterwards: on
     * a speaker the narration would otherwise be recorded into the memory, and
     * in headphones the traveller would be talking over a voice in their ear.
     * The pause is local — it is not published to the group, because screen 09
     * is the only surface that speaks for the group (D049, D058).
     */
    fun start(attribution: MemoryAttribution) {
        if (_state.value.isActive) return

        val id = newId()
        pauseGuideForRecording()

        val output = fileFor(id)
        if (!recorder.start(output)) {
            resumeGuideIfPaused()
            _state.update { it.failRecording(MemoryFailure.RecorderFailed, now()) }
            return
        }

        _state.update { it.startRecording(id, now(), epochNow(), attribution) }
        startTicker()
    }

    /** The traveller refused the microphone. Costs the recording, not the screen. */
    fun microphoneRefused() {
        _state.update { it.failRecording(MemoryFailure.NoMicrophonePermission, now()) }
    }

    fun pause() {
        if (_state.value.phase != MemoryPhase.Recording) return
        recorder.pause()
        _state.update { it.pauseRecording(now()) }
        stopTicker()
    }

    fun resume() {
        if (_state.value.phase != MemoryPhase.Paused) return
        recorder.resume()
        _state.update { it.resumeRecording(now()) }
        startTicker()
    }

    /**
     * "Concluir", and the one path this whole class exists to get right.
     *
     * Past `recorder.stop()` the audio is on disk. Everything after that is
     * bookkeeping, and no failure in it is allowed to throw the recording
     * away: the row not being written is reported as
     * [MemoryFailure.SavedFileOnly], which says the memory is still there
     * (brief §22).
     */
    fun finish() {
        val recording = _state.value
        if (!recording.isActive) return

        stopTicker()
        val finalized = recorder.stop()
        resumeGuideIfPaused()

        if (!finalized) {
            _state.update { it.failRecording(MemoryFailure.RecorderFailed, now()) }
            return
        }

        val done = recording.finishRecording(now())
        _state.value = done

        val id = done.memoryId ?: return
        val who = done.attribution
        scope.launch {
            val stored = runCatching {
                memories.save(
                    Memory(
                        id = id,
                        fileName = fileFor(id).name,
                        recordedAtEpochMs = done.startedAtEpochMs ?: epochNow(),
                        durationMs = done.bankedMs,
                        participantId = who?.participantId.orEmpty(),
                        participantName = who?.participantName.orEmpty(),
                        cityName = who?.cityName,
                        placeName = who?.placeName,
                    ),
                )
            }.isSuccess
            if (!stored) {
                _state.update { it.failRecording(MemoryFailure.SavedFileOnly, now()) }
            }
        }
    }

    /** "Cancelar" — the only path that throws audio away, and it is a tap. */
    fun cancel() {
        if (!_state.value.isActive) return
        stopTicker()
        recorder.cancel()
        resumeGuideIfPaused()
        _state.update { it.cancelRecording() }
    }

    /** Leaving the confirmation, or the error, behind. */
    fun acknowledge() {
        _state.update { it.acknowledge() }
    }

    /** The file a memory's audio lives in, per brief §22. */
    fun fileFor(id: String): File = File(File(filesDir(), MEMORY_DIR), "$id$EXTENSION")

    private fun pauseGuideForRecording() {
        pausedTheGuide = playback.state.value.isPlaying
        if (pausedTheGuide) playback.pause()
    }

    private fun resumeGuideIfPaused() {
        if (!pausedTheGuide) return
        pausedTheGuide = false
        playback.play()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                _level.value = recorder.level()
                // The screen reads elapsed time off the state with its own
                // clock, so this only has to keep the waveform alive.
                delay(LEVEL_TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
        _level.value = 0f
    }

    private companion object {
        const val MEMORY_DIR = "memories"
        const val EXTENSION = ".m4a"
        const val LEVEL_TICK_MS = 90L
    }
}
