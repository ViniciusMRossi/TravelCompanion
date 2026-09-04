package com.travelcompanion.app.service.memory

import android.media.MediaPlayer
import java.io.File

/**
 * Playing back a voice memory, behind an interface.
 *
 * Deliberately *not* the app's `PlaybackController`. An audioguide exists to
 * survive a pocket and a dark screen — which is why D015/D017 put it on the
 * application, in a media session, with a notification. A memory is a
 * forty-second recording listened to while looking at the list, its progress
 * bar drawn inside the row itself; it never belongs to a media session, a
 * notification or a lock screen, where a personal recording would be
 * announced to anyone walking past the phone (D081).
 *
 * Small enough that everything above it can be tested with a fake.
 */
interface MemoryAudioPlayer {

    /** Starts [file] from the beginning. Returns false if it will not play. */
    fun play(file: File, onCompleted: () -> Unit): Boolean

    fun pause()

    /** Resumes what was paused. Returns false when there is nothing to resume. */
    fun resume(): Boolean

    /** Releases everything. Safe to call when nothing is playing. */
    fun stop()

    val isPlaying: Boolean
    val positionMs: Long
    val durationMs: Long
}

/**
 * [MemoryAudioPlayer] over `MediaPlayer`, on a local file.
 *
 * No streaming, no buffering state and no network: the file is on this phone
 * or the row does not exist.
 */
class MediaMemoryPlayer : MemoryAudioPlayer {

    private var player: MediaPlayer? = null

    override fun play(file: File, onCompleted: () -> Unit): Boolean {
        stop()
        if (!file.exists()) return false
        return runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { onCompleted() }
                prepare()
                start()
            }
        }.onSuccess { player = it }.isSuccess
    }

    override fun pause() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    override fun resume(): Boolean {
        val current = player ?: return false
        return runCatching { current.start() }.isSuccess
    }

    override fun stop() {
        val current = player ?: return
        player = null
        runCatching { current.release() }
    }

    override val isPlaying: Boolean
        get() = runCatching { player?.isPlaying == true }.getOrDefault(false)

    override val positionMs: Long
        get() = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    override val durationMs: Long
        get() = runCatching { player?.duration?.toLong()?.coerceAtLeast(0L) ?: 0L }.getOrDefault(0L)
}
