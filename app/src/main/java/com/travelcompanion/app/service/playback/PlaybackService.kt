package com.travelcompanion.app.service.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.travelcompanion.app.MainActivity

/**
 * Hosts the player so audio outlives the screen that started it.
 *
 * A MediaSessionService is what gives the traveller notification and
 * lock-screen controls and makes headset buttons work — all of which the walk
 * depends on, because the phone is in a pocket.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Pause instead of playing out loud when headphones are unplugged.
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, withoutQueueCommands(player))
            .setSessionActivity(openApp())
            .build()
    }

    /**
     * Where tapping the notification or the lock-screen control leads.
     *
     * Launcher semantics on purpose: the traveller is returned to the task
     * they already have, on the screen they left, instead of to a second copy
     * of the app with an empty back stack. NEW_TASK alone is not enough for
     * that — MainActivity launches as `standard`, so it would stack a fresh
     * copy showing Hoje on top of the screen the traveller was actually on;
     * SINGLE_TOP reuses the instance that is already there. The intent carries
     * nothing, so it is immutable.
     */
    private fun openApp(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Drops the commands that only mean something with a queue.
     *
     * An audioguide is a single item, so "previous" and "next" have nowhere to
     * go, yet Media3 puts a previous button on the notification and advertises
     * skip actions on the platform session the lock screen and Bluetooth read.
     * It does so purely because the player reports the commands:
     * `DefaultMediaNotificationProvider` builds its buttons from
     * `player.getAvailableCommands()`, and media button preferences can only
     * replace that button, never remove it. So the narrowing belongs on the
     * player, where every surface reads it from.
     *
     * This removes nothing the app uses: playback sets a single media item and
     * seeks within it — which is how a finished guide replays — and both of
     * those commands stay. It does not add ±15 or chapter controls either;
     * those remain deferred (D025).
     */
    @OptIn(UnstableApi::class)
    private fun withoutQueueCommands(player: Player): Player =
        object : ForwardingSimpleBasePlayer(player) {
            override fun getState(): SimpleBasePlayer.State {
                val state = super.getState()
                return state.buildUpon()
                    .setAvailableCommands(
                        state.availableCommands.buildUpon()
                            .removeAll(
                                Player.COMMAND_SEEK_TO_PREVIOUS,
                                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                                Player.COMMAND_SEEK_TO_NEXT,
                                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                            )
                            .build(),
                    )
                    .build()
            }
        }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away while paused should not leave a dead
        // notification behind; while playing, the session keeps going.
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            // Releases the wrapper, which releases the ExoPlayer underneath it.
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
