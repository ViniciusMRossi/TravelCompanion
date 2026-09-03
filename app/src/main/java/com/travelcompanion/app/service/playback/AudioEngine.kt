package com.travelcompanion.app.service.playback

/**
 * The seam between [PlaybackController] and Media3.
 *
 * It exists so playback coordination — restoring a saved position, deciding
 * what the traveller sees, persisting progress — can be exercised without a
 * device, and so nothing above this line depends on ExoPlayer. It is
 * deliberately narrow: only the operations the app actually performs.
 */
interface AudioEngine {

    fun setListener(listener: Listener?)

    /**
     * Loads [uri] and seeks to [startPositionMs]. Does not start playback.
     *
     * [uri] is always a local `asset:///` URI — audio is never streamed.
     * [title] and [subtitle] are what the notification and lock screen show,
     * so they must be the traveller's words, never an internal id.
     */
    fun prepare(
        mediaId: String,
        uri: String,
        title: String,
        subtitle: String?,
        startPositionMs: Long,
    )

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    /** Stops playback and clears the item, leaving the session alive. */
    fun stop()

    val positionMs: Long

    val durationMs: Long

    val isPlaying: Boolean

    interface Listener {
        fun onBuffering()

        /** Ready to play; [durationMs] is known from here on. */
        fun onReady(durationMs: Long)

        fun onPlayingChanged(isPlaying: Boolean)

        fun onEnded()

        fun onError(cause: Throwable?)
    }
}
