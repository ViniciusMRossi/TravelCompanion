package com.travelcompanion.app.service.playback

/**
 * Stand-in for Media3 so playback coordination can be tested without a device.
 *
 * It records what the controller asked for and lets a test drive the callbacks
 * a real player would raise.
 */
class FakeAudioEngine : AudioEngine {

    var preparedMediaId: String? = null
        private set
    var preparedUri: String? = null
        private set
    var preparedStartPositionMs: Long = -1L
        private set
    var prepareCount: Int = 0
        private set
    var stopCount: Int = 0
        private set
    var preparedTitle: String? = null
        private set
    var preparedSubtitle: String? = null
        private set
    var released: Boolean = false
        private set

    /** Every command the controller issued, in order. */
    val commands = mutableListOf<String>()

    private var listener: AudioEngine.Listener? = null

    override var positionMs: Long = 0L
    override var durationMs: Long = 0L
    override var isPlaying: Boolean = false
        private set

    override fun setListener(listener: AudioEngine.Listener?) {
        this.listener = listener
    }

    override fun prepare(
        mediaId: String,
        uri: String,
        title: String,
        subtitle: String?,
        startPositionMs: Long,
    ) {
        prepareCount++
        preparedMediaId = mediaId
        preparedUri = uri
        preparedTitle = title
        preparedSubtitle = subtitle
        preparedStartPositionMs = startPositionMs
        positionMs = startPositionMs
        commands += "prepare"
        listener?.onBuffering()
    }

    override fun play() {
        isPlaying = true
        commands += "play"
        listener?.onPlayingChanged(true)
    }

    override fun pause() {
        isPlaying = false
        commands += "pause"
        listener?.onPlayingChanged(false)
    }

    override fun seekTo(positionMs: Long) {
        this.positionMs = positionMs
        commands += "seekTo($positionMs)"
    }

    override fun stop() {
        stopCount++
        isPlaying = false
        positionMs = 0L
        commands += "stop"
    }

    /** Simulates the player finishing preparation. */
    fun becomeReady(durationMs: Long) {
        this.durationMs = durationMs
        listener?.onReady(durationMs)
    }

    fun advanceTo(positionMs: Long) {
        this.positionMs = positionMs
    }

    fun finish() {
        isPlaying = false
        listener?.onEnded()
    }

    fun fail(cause: Throwable = IllegalStateException("decode failed")) {
        isPlaying = false
        listener?.onError(cause)
    }

    /** Raises a bare callback, without the command that normally precedes it. */
    fun emitPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        listener?.onPlayingChanged(isPlaying)
    }

    fun emitEnded() {
        listener?.onEnded()
    }

    fun markReleased() {
        released = true
    }
}

/** In-memory [PlaybackPositionStore] for tests. */
class InMemoryPlaybackPositionStore(
    initial: Map<String, Long> = emptyMap(),
) : PlaybackPositionStore {

    private val positions = initial.toMutableMap()

    override suspend fun load(mediaId: String): Long = positions[mediaId] ?: 0L

    override suspend fun save(mediaId: String, positionMs: Long) {
        positions[mediaId] = positionMs
    }

    override suspend fun clear(mediaId: String) {
        positions.remove(mediaId)
    }

    fun peek(mediaId: String): Long? = positions[mediaId]
}
