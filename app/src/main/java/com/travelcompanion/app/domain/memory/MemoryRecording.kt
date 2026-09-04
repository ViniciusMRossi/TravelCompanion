package com.travelcompanion.app.domain.memory

/**
 * Where a recording is. The three states the approved sheet draws, plus the
 * two a recorder can actually be in that it does not draw: paused, and failed.
 */
enum class MemoryPhase { Idle, Recording, Paused, Saved, Failed }

/**
 * What went wrong, in a form the screen can say out loud.
 *
 * Deliberately not an exception: brief §22 asks for an understandable error,
 * and the one case that matters is the last one — the audio exists and the
 * row does not, which must never read as "nothing happened".
 */
enum class MemoryFailure {
    /** The microphone was refused. Costs the recording, not the screen (D030). */
    NoMicrophonePermission,

    /** The recorder would not start, or stopped badly enough to lose the file. */
    RecorderFailed,

    /** The audio is on disk and its row is not. The recording is not lost. */
    SavedFileOnly,
}

/**
 * Recording as a value.
 *
 * Every transition below is a pure function, so the machine can be driven with
 * a fake clock and no microphone — the same shape `domain/walk` and
 * `domain/sync` have, and for the same reason: this is where the real risk
 * lives, and hardware is the one thing a test cannot have.
 *
 * Time is kept as *banked* milliseconds plus the run in progress, rather than
 * as a start timestamp, because pausing is part of the approved flow and a
 * single start time cannot survive it.
 */
data class MemoryRecordingState(
    val phase: MemoryPhase = MemoryPhase.Idle,
    val memoryId: String? = null,
    /** Milliseconds already recorded, not counting the run in progress. */
    val bankedMs: Long = 0L,
    /** Monotonic reading at which the run in progress began, or null. */
    val runStartedAtMs: Long? = null,
    /** Wall clock of the moment the traveller tapped record. */
    val startedAtEpochMs: Long? = null,
    val attribution: MemoryAttribution? = null,
    val failure: MemoryFailure? = null,
) {
    /** How long has been recorded, at [nowMs] on the same monotonic clock. */
    fun elapsedMs(nowMs: Long): Long =
        bankedMs + (runStartedAtMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L)

    /** True while there is a recording to pause, resume, finish or cancel. */
    val isActive: Boolean get() = phase == MemoryPhase.Recording || phase == MemoryPhase.Paused
}

/**
 * Who was recording and where, captured when the recording starts.
 *
 * Where, not who-is-nearby: this is the packaged trip plus the walk this phone
 * is on, never anything asked of the group. Nothing here leaves the device.
 */
data class MemoryAttribution(
    val participantId: String,
    val participantName: String,
    val cityName: String?,
    val placeName: String?,
)

/** Tapping the record button. Only from a machine that is not already going. */
fun MemoryRecordingState.startRecording(
    memoryId: String,
    nowMs: Long,
    epochMs: Long,
    attribution: MemoryAttribution,
): MemoryRecordingState {
    if (isActive) return this
    return MemoryRecordingState(
        phase = MemoryPhase.Recording,
        memoryId = memoryId,
        bankedMs = 0L,
        runStartedAtMs = nowMs,
        startedAtEpochMs = epochMs,
        attribution = attribution,
    )
}

/** "Pausar". Banks the run in progress; the recording is still going on. */
fun MemoryRecordingState.pauseRecording(nowMs: Long): MemoryRecordingState {
    if (phase != MemoryPhase.Recording) return this
    return copy(phase = MemoryPhase.Paused, bankedMs = elapsedMs(nowMs), runStartedAtMs = null)
}

fun MemoryRecordingState.resumeRecording(nowMs: Long): MemoryRecordingState {
    if (phase != MemoryPhase.Paused) return this
    return copy(phase = MemoryPhase.Recording, runStartedAtMs = nowMs)
}

/**
 * "Concluir". The one transition that must not be reversible: past here the
 * audio exists, and whatever else fails, it is not thrown away (brief §22).
 */
fun MemoryRecordingState.finishRecording(nowMs: Long): MemoryRecordingState {
    if (!isActive) return this
    return copy(phase = MemoryPhase.Saved, bankedMs = elapsedMs(nowMs), runStartedAtMs = null)
}

/** "Cancelar". The only path that discards audio, and it is explicit. */
fun MemoryRecordingState.cancelRecording(): MemoryRecordingState {
    if (!isActive) return this
    return MemoryRecordingState()
}

/**
 * Something went wrong.
 *
 * [MemoryFailure.SavedFileOnly] keeps the elapsed time and the identity,
 * because the recording it describes still exists; the others give up the run.
 */
fun MemoryRecordingState.failRecording(
    failure: MemoryFailure,
    nowMs: Long,
): MemoryRecordingState = when (failure) {
    MemoryFailure.SavedFileOnly -> copy(
        phase = MemoryPhase.Failed,
        bankedMs = elapsedMs(nowMs),
        runStartedAtMs = null,
        failure = failure,
    )
    else -> MemoryRecordingState(phase = MemoryPhase.Failed, failure = failure)
}

/** Dismissing the confirmation, or the error, and going back to rest. */
fun MemoryRecordingState.acknowledge(): MemoryRecordingState =
    if (phase == MemoryPhase.Saved || phase == MemoryPhase.Failed) MemoryRecordingState() else this
