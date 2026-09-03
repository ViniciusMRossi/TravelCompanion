package com.travelcompanion.app.service.walk

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * [WalkPresence] against the real Android services.
 *
 * Every call here is best-effort on purpose. Posting a notification can be
 * refused — the traveller may have declined POST_NOTIFICATIONS — and starting
 * a foreground service can be refused too. Neither may end a walk or stop
 * audio, so a refusal is swallowed rather than thrown at the controller.
 */
class AndroidWalkPresence(context: Context) : WalkPresence {

    private val appContext = context.applicationContext

    override fun startWalk(title: String, text: String) {
        runCatching {
            ContextCompat.startForegroundService(
                appContext,
                WalkModeService.intent(appContext, title, text),
            )
        }
    }

    override fun updateWalk(title: String, text: String) {
        post(WalkNotifications.WALK_NOTIFICATION_ID) {
            WalkNotifications.walkNotification(appContext, title, text)
        }
    }

    override fun stopWalk() {
        runCatching { appContext.stopService(WalkModeService.intent(appContext, "", "")) }
        // Cancelled explicitly, not left to the service. Stopping a foreground
        // service removes the notification it owns, but the walk notification
        // is not always owned by one: without a location permission no service
        // is started, and a walk can still post progress. Left behind it would
        // be an ONGOING|NO_CLEAR notification for a walk that ended, which the
        // traveller cannot dismiss.
        runCatching {
            NotificationManagerCompat.from(appContext)
                .cancel(WalkNotifications.WALK_NOTIFICATION_ID)
        }
    }

    override fun notifyStory(storyId: String, title: String, hook: String) {
        post(WalkNotifications.storyNotificationId(storyId)) {
            WalkNotifications.storyNotification(appContext, title, hook)
        }
    }

    /**
     * Posts, or does nothing at all when notifications were refused.
     *
     * The check is inline rather than behind a helper so it is the same
     * statement lint reads and the reader reads.
     */
    private fun post(id: Int, build: () -> Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            WalkNotifications.ensureChannels(appContext)
            NotificationManagerCompat.from(appContext).notify(id, build())
        }
    }
}
