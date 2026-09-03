package com.travelcompanion.app.service.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns audioguide playback for the whole app.
 *
 * It lives on the application, not on a screen: navigating away from the
 * attraction must not interrupt the audio, and the traveller's phone is
 * usually in a pocket by then. Screens observe [state] and send intents.
 *
 * Nothing here talks to the network or to group sync. Local playback is not
 * allowed to depend on either.
 */
class PlaybackController(
    private val engine: AudioEngine,
    private val positions: PlaybackPositionStore,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var ticker: Job? = null

    init {
        engine.setListener(EngineEvents())
    }

    /**
     * Prepares a guide and starts it, resuming where it was left.
     *
     * Asking for the guide that is already loaded toggles instead of
     * restarting it — tapping the same action twice should not lose the place.
     */
    fun playAudioGuide(request: AudioGuideRequest) {
        // Policy: a guide whose audio is not packaged is not playable, and it
        // must not disturb audio that *is* playing. Availability is knowable
        // from content before anyone taps, so the screen tells the traveller
        // up front instead of the player pretending to fail afterwards.
        val playable = request as? AudioGuideRequest.Playable ?: return

        if (_state.value.isFor(playable.mediaId) && _state.value.isActive) {
            // A finished guide restarts; anything else toggles.
            if (_state.value.status == PlaybackState.Status.Ended) restart() else togglePlayPause()
            return
        }

        persistCurrentPosition()

        scope.launch {
            val resumeFrom = positions.load(playable.mediaId)
            _state.value = PlaybackState(
                mediaId = playable.mediaId,
                title = playable.title,
                subtitle = playable.subtitle,
                status = PlaybackState.Status.Buffering,
                positionMs = resumeFrom,
                durationMs = playable.declaredDurationMs,
                chapters = playable.chapters,
            )
            engine.prepare(
                mediaId = playable.mediaId,
                uri = playable.uri,
                title = playable.title,
                subtitle = playable.subtitle,
                startPositionMs = resumeFrom,
            )
            engine.play()
        }
    }

    /** Plays a finished guide again from the top. */
    private fun restart() {
        engine.seekTo(0L)
        _state.update { it.copy(positionMs = 0L, status = PlaybackState.Status.Buffering) }
        engine.play()
    }

    fun togglePlayPause() {
        if (!_state.value.isActive) return
        when {
            // The play button on a finished guide means "again", not "resume
            // from the end", where nothing would happen.
            _state.value.status == PlaybackState.Status.Ended -> restart()
            engine.isPlaying -> pause()
            else -> play()
        }
    }

    fun play() {
        if (!_state.value.isActive) return
        engine.play()
    }

    fun pause() {
        if (!_state.value.isActive) return
        engine.pause()
        persistCurrentPosition()
    }

    fun seekTo(positionMs: Long) {
        if (!_state.value.isActive) return
        val bounded = positionMs.coerceIn(0L, maxOf(_state.value.durationMs, 0L))
        engine.seekTo(bounded)
        _state.update { it.copy(positionMs = bounded) }
        persistCurrentPosition(bounded)
    }

    /** −15 / +15 on the approved transport. */
    fun seekBy(deltaMs: Long) = seekTo(_state.value.positionMs + deltaMs)

    /**
     * Jumps to a chapter of the guide that is loaded.
     *
     * The approved chapter list lives on screen 04 (Cidade); this is the
     * capability it will drive, and what the player's chapter line reflects.
     */
    fun seekToChapter(index: Int) {
        val chapters = _state.value.chapters
        val chapter = chapters.getOrNull(index) ?: return
        seekTo(chapter.startMs)
    }

    fun skipToNextChapter() {
        val current = _state.value.currentChapterIndex ?: return
        seekToChapter(current + 1)
    }

    fun skipToPreviousChapter() {
        val current = _state.value.currentChapterIndex ?: return
        seekToChapter(current - 1)
    }

    fun stop() {
        persistCurrentPosition()
        stopTicker()
        engine.stop()
        _state.value = PlaybackState()
    }

    /** Samples the engine. Production ticks this while playing. */
    fun refreshProgress() {
        if (!_state.value.isActive) return
        _state.update {
            it.copy(
                positionMs = engine.positionMs,
                durationMs = if (engine.durationMs > 0L) engine.durationMs else it.durationMs,
            )
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                refreshProgress()
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun persistCurrentPosition(positionMs: Long = _state.value.positionMs) {
        val mediaId = _state.value.mediaId ?: return
        scope.launch { positions.save(mediaId, positionMs) }
    }

    private inner class EngineEvents : AudioEngine.Listener {

        override fun onBuffering() {
            _state.update {
                // Buffering after a failure is meaningless; after Ended it is a
                // replay getting under way, which is real progress.
                if (it.status == PlaybackState.Status.Failed) it
                else it.copy(status = PlaybackState.Status.Buffering)
            }
        }

        override fun onReady(durationMs: Long) {
            _state.update {
                it.copy(
                    durationMs = if (durationMs > 0L) durationMs else it.durationMs,
                    status = if (it.status == PlaybackState.Status.Buffering) {
                        PlaybackState.Status.Paused
                    } else {
                        it.status
                    },
                    failure = null,
                )
            }
        }

        override fun onPlayingChanged(isPlaying: Boolean) {
            // In Media3 `false` is ambiguous: paused, buffering, ended, failed,
            // stopped, or playback suppressed. Ended and Failed already say
            // something more specific and arrive in either order relative to
            // this callback, so they must never be downgraded to Paused.
            val wasTerminal = _state.value.isTerminal

            _state.update {
                when {
                    isPlaying -> it.copy(
                        status = PlaybackState.Status.Playing,
                        positionMs = engine.positionMs,
                        failure = null,
                    )
                    it.isTerminal -> it
                    else -> it.copy(
                        status = PlaybackState.Status.Paused,
                        positionMs = engine.positionMs,
                    )
                }
            }

            if (isPlaying) startTicker() else stopTicker()
            // A guide that ended keeps no resume point; do not write one back.
            if (!isPlaying && !wasTerminal) persistCurrentPosition()
        }

        override fun onEnded() {
            stopTicker()
            val mediaId = _state.value.mediaId
            _state.update {
                // Ended wins over a Paused that a preceding onPlayingChanged
                // may already have written.
                if (it.status == PlaybackState.Status.Failed) it
                else it.copy(status = PlaybackState.Status.Ended, positionMs = it.durationMs)
            }
            // A finished guide starts from the beginning next time.
            if (mediaId != null) scope.launch { positions.clear(mediaId) }
        }

        override fun onError(cause: Throwable?) {
            stopTicker()
            _state.update {
                it.copy(
                    status = PlaybackState.Status.Failed,
                    failure = PlaybackFailure.PlaybackFailed,
                )
            }
        }
    }

    private companion object {
        const val PROGRESS_TICK_MS = 500L
    }
}
