package com.travelcompanion.app.service.memory

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * The microphone, behind an interface.
 *
 * Every decision about recording is made in `domain/memory` and driven from
 * [MemoryController]; this is only the part that cannot be tested without
 * hardware, kept as small as that allows. A fake stands in everywhere else.
 */
interface AudioRecorder {

    /** Begins writing to [output]. Returns false if the recorder would not start. */
    fun start(output: File): Boolean

    fun pause()
    fun resume()

    /**
     * Finishes and closes the file. Returns whether the file is usable.
     *
     * Never throws: a recording that has already been made must not be lost to
     * an exception on the way out (brief §22).
     */
    fun stop(): Boolean

    /** Abandons the recording and deletes what was written. */
    fun cancel()

    /** 0..1, for the waveform. Zero when nothing is being recorded. */
    fun level(): Float
}

/**
 * [AudioRecorder] over `MediaRecorder`, writing AAC in an m4a container.
 *
 * m4a because brief §22 names it, and because it is the format that survives
 * being handed to anything else later.
 */
class MediaAudioRecorder(private val context: Context) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var target: File? = null

    override fun start(output: File): Boolean {
        cancel()
        output.parentFile?.mkdirs()
        val created = runCatching {
            @Suppress("DEPRECATION")
            val next = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            next.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(BIT_RATE)
                setAudioSamplingRate(SAMPLE_RATE)
                setOutputFile(output.absolutePath)
                prepare()
                start()
            }
        }.getOrNull()

        recorder = created
        target = output.takeIf { created != null }
        return created != null
    }

    override fun pause() {
        runCatching { recorder?.pause() }
    }

    override fun resume() {
        runCatching { recorder?.resume() }
    }

    override fun stop(): Boolean {
        val active = recorder ?: return false
        // `stop` throws when the recording is too short to have written a
        // valid file. That is a failure to finalize, not something to crash on.
        val finalized = runCatching { active.stop() }.isSuccess
        runCatching { active.release() }
        recorder = null
        val file = target
        target = null
        if (!finalized) runCatching { file?.delete() }
        return finalized && file != null && file.length() > 0L
    }

    override fun cancel() {
        val active = recorder
        recorder = null
        if (active != null) {
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        runCatching { target?.delete() }
        target = null
    }

    override fun level(): Float {
        val active = recorder ?: return 0f
        val amplitude = runCatching { active.maxAmplitude }.getOrDefault(0)
        return (amplitude / MAX_AMPLITUDE).coerceIn(0f, 1f)
    }

    private companion object {
        const val BIT_RATE = 96_000
        const val SAMPLE_RATE = 44_100

        /** `maxAmplitude` is 16-bit; speech sits well below the ceiling. */
        const val MAX_AMPLITUDE = 12_000f
    }
}
