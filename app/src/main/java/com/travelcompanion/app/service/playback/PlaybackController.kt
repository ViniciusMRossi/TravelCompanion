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
    private var stopping = false

    init {
        engine.setListener(EngineEvents())
    }

    /**
     * Prepares a guide and starts it, resuming where it was left.
     *
     * Asking for the guide that is already loaded toggles instead of
     * restarting it — tapping the same action twice should not lose the place.
     *
     * [startPositionMs] overrides that resumed position for the one caller
     * that knows better: joining a shared listen has to begin where the group
     * is, not where this phone last left the guide. Passing it here rather
     * than seeking afterwards keeps it a single decision — the preparation
     * happens off the caller's stack, so a seek issued straight after would
     * race the load it is meant to correct.
     */
    fun playAudioGuide(request: AudioGuideRequest, startPositionMs: Long? = null) {
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
            val resumeFrom = startPositionMs ?: positions.load(playable.mediaId)
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
        // What was asked for, recorded before the engine is asked — the same
        // shape [seekTo] already has, and for the same reason: a caller that
        // acts on this state in the next statement must not depend on when a
        // listener answers. Screen 09 telling the group what its transport did
        // is such a caller, and it publishes `isPlaying` (D050).
        //
        // Never over a terminal state: Ended and Failed say something more
        // specific, which is the rule [EngineEvents.onPlayingChanged] already
        // keeps. A synchronous callback arriving inside `engine.play()` — a
        // buffering report, say — lands after this and wins, because what the
        // engine reports is truer than what was asked for.
        _state.update { if (it.isTerminal) it else it.copy(status = PlaybackState.Status.Playing) }
        engine.play()
    }

    fun pause() {
        if (!_state.value.isActive) return
        _state.update { if (it.isTerminal) it else it.copy(status = PlaybackState.Status.Paused) }
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

    /**
     * Ends the session: no guide loaded, no compact player, nothing claimed.
     *
     * It is not destructive. The resume point is written before the state is
     * cleared, and [playAudioGuide] reads it back with `positions.load`, so
     * asking for the same guide again continues where it stopped rather than
     * starting over. Only [EngineEvents.onEnded] discards a position, because
     * a guide that finished has none.
     *
     * The guard is not decoration: `engine.stop()` is the very thing the
     * session reports back as [EngineEvents.onStopped], so without it this
     * method calls itself from inside its own third line.
     */
    fun stop() {
        if (stopping) return
        stopping = true
        try {
            persistCurrentPosition()
            stopTicker()
            engine.stop()
            _state.value = PlaybackState()
        } finally {
            stopping = false
        }
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

        /**
         * The session ended without this app asking: the notification was
         * dismissed, or the session went away.
         *
         * Propagated to the same [stop] a button would call — the state is
         * where "there is no audio" is said, and a second path saying it would
         * be a second answer to the same question.
         *
         * Failure is not a stop. The player reaches IDLE after an error too,
         * and `MediaControllerImplBase` delivers `onPlayerError` before the
         * state change that follows it, so Failed is already on the state by
         * the time this arrives and must survive: it is what screen 05 reads
         * to tell the traveller the guide could not be played.
         */
        override fun onStopped() {
            if (_state.value.status == PlaybackState.Status.Failed) return
            if (_state.value.mediaId == null) return
            stop()
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
