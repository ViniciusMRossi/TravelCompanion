package com.travelcompanion.app.service.walk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.travelcompanion.app.MainActivity
import com.travelcompanion.app.R

/**
 * The Walk and Stories channels from the brief's notification list.
 *
 * Both are local: nothing here touches the network, so an operational or story
 * notification behaves the same with the radios off.
 */
object WalkNotifications {

    const val WALK_CHANNEL_ID = "walk"
    const val STORIES_CHANNEL_ID = "stories"

    /** The one the foreground service posts; there is only ever one walk. */
    const val WALK_NOTIFICATION_ID = 2001

    /** Story notifications are keyed by story so a second one cannot stack up. */
    fun storyNotificationId(storyId: String): Int = 3000 + (storyId.hashCode() and 0x0FFF)

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Low importance: the walk notification is a requirement of running in
        // the background, not something to interrupt a walk with.
        manager.createNotificationChannel(
            NotificationChannel(
                WALK_CHANNEL_ID,
                context.getString(R.string.walk_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.walk_channel_description) },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                STORIES_CHANNEL_ID,
                context.getString(R.string.stories_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.stories_channel_description) },
        )
    }

    /**
     * Returning to the walk the traveller already has, on the screen they left
     * — the same launcher semantics the playback notification uses.
     */
    fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * The persistent notification Android requires while a walk holds location
     * in the foreground. It says what the walk is doing, not what the app is.
     */
    fun walkNotification(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, WALK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    /** Outside a walk, a story is an offer the traveller can ignore. */
    fun storyNotification(context: Context, title: String, hook: String): Notification =
        NotificationCompat.Builder(context, STORIES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk_notification)
            .setContentTitle(title)
            .setContentText(hook)
            .setStyle(NotificationCompat.BigTextStyle().bigText(hook))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
}
