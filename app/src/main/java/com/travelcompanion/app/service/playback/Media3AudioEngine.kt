package com.travelcompanion.app.service.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.util.concurrent.Executor

/**
 * Media3 implementation of [AudioEngine].
 *
 * Connects to [PlaybackService] lazily — the service (and its notification)
 * should only exist once the traveller actually asks for audio. Commands
 * issued while connecting are queued in order by [ConnectionQueue], so a
 * `prepare` is never overtaken by the `play` that follows it.
 */
class Media3AudioEngine(context: Context) : AudioEngine {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = Executor { command -> mainHandler.post(command) }

    private val connection = ConnectionQueue<MediaController>()
    private var connecting = false

    private var listener: AudioEngine.Listener? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> listener?.onBuffering()
                Player.STATE_READY -> listener?.onReady(durationMs)
                Player.STATE_ENDED -> listener?.onEnded()
                // The session player only reaches IDLE by being stopped, by
                // failing, or by going away with the service. Dismissing the
                // media notification is the first of those: Media3 builds the
                // notification's delete intent from `COMMAND_STOP`
                // (`DefaultActionFactory.createNotificationDismissalIntent`),
                // and 1.10.1 exposes no callback for the dismissal itself —
                // `MediaNotificationManager.onNotificationDismissed` is
                // private and only records a flag. So the stop the player
                // performs *is* the hook, and it belongs here, on the seam the
                // app already learns about Media3 through.
                Player.STATE_IDLE -> listener?.onStopped()
                else -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listener?.onPlayingChanged(isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onError(error)
        }
    }

    override fun setListener(listener: AudioEngine.Listener?) {
        this.listener = listener
    }

    override fun prepare(
        mediaId: String,
        uri: String,
        title: String,
        subtitle: String?,
        startPositionMs: Long,
    ) = submit { player ->
        val item = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(
                // What the notification and lock screen show.
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setDisplayTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
        player.setMediaItem(item, startPositionMs)
        player.prepare()
    }

    override fun play() = submit { it.play() }

    override fun pause() = submit { it.pause() }

    override fun seekTo(positionMs: Long) = submit { it.seekTo(positionMs) }

    override fun stop() = submit {
        it.stop()
        it.clearMediaItems()
    }

    override val positionMs: Long
        get() = connection.current()?.currentPosition?.coerceAtLeast(0L) ?: 0L

    override val durationMs: Long
        get() = connection.current()?.duration?.takeIf { it > 0L } ?: 0L

    override val isPlaying: Boolean
        get() = connection.current()?.isPlaying == true

    private fun submit(action: (MediaController) -> Unit) {
        connection.submit(action)
        if (connection.isConnected) return
        connect()
    }

    private fun connect() {
        if (connecting) return
        connecting = true

        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token)
            .setListener(connectionListener)
            .buildAsync()

        future.addListener(
            {
                connecting = false
                val controller = runCatching { future.get() }.getOrNull()
                if (controller == null) {
                    // Nothing will ever run these, and replaying a stale
                    // prepare/play after a later retry would be worse than
                    // losing them. Drop the queue and report the failure.
                    connection.detach()
                    listener?.onError(IllegalStateException("media session unavailable"))
                    return@addListener
                }
                controller.addListener(playerListener)
                // Drains prepare, then play, in the order they were issued.
                connection.attach(controller)
            },
            mainExecutor,
        )
    }

    /**
     * The session can go away underneath us — the service is stopped, or the
     * process hosting it dies. Drop the controller so the next command
     * reconnects from scratch instead of talking to a dead session.
     */
    private val connectionListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            controller.removeListener(playerListener)
            connection.detach()
        }
    }

    fun release() {
        connection.current()?.let {
            it.removeListener(playerListener)
            it.release()
        }
        connection.detach()
        connecting = false
    }
}
