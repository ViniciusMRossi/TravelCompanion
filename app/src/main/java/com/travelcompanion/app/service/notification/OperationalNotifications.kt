package com.travelcompanion.app.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.travelcompanion.app.MainActivity
import com.travelcompanion.app.R
import com.travelcompanion.app.domain.alerts.AlertTarget
import com.travelcompanion.app.feature.shell.Routes

/**
 * The Operational and Memory channels from the brief's notification list.
 *
 * Local, like Walk and Stories: nothing here reads the network, so a deadline
 * announces itself the same with the radios off — which the brief asks for
 * literally, and which is the state this trip is mostly in.
 *
 * **Memory posts nothing in this delivery.** The channel exists so the
 * traveller can switch it off before it ever speaks; what the evening prompt
 * would say is copy no approved sheet draws, and it went to the design
 * confirmation stack rather than being invented (D094).
 */
object OperationalNotifications {

    const val OPERATIONAL_CHANNEL_ID = "operational"
    const val MEMORY_CHANNEL_ID = "memory"

    /** Keyed by the critical item, so a rescheduled deadline replaces itself. */
    fun notificationId(criticalItemId: String): Int = 4000 + (criticalItemId.hashCode() and 0x0FFF)

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // High: this is the one notification in the app that exists because
        // missing it costs a flight. It is allowed to interrupt.
        manager.createNotificationChannel(
            NotificationChannel(
                OPERATIONAL_CHANNEL_ID,
                context.getString(R.string.operational_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.operational_channel_description) },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                MEMORY_CHANNEL_ID,
                context.getString(R.string.memory_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.memory_channel_description) },
        )
    }

    /**
     * The deadline, in the packaged words and no others.
     *
     * Title and body are `criticalItem.title` and `criticalItem.instruction`.
     * Nothing is composed here: a sentence written for a traveller under time
     * pressure is content, and the content already has it.
     */
    fun deadlineNotification(
        context: Context,
        title: String,
        instruction: String,
        route: String?,
    ): Notification =
        NotificationCompat.Builder(context, OPERATIONAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk_notification)
            .setContentTitle(title)
            .setContentText(instruction)
            // The instruction is a full sentence and often two; a collapsed
            // line would cut it exactly where the useful half begins.
            .setStyle(NotificationCompat.BigTextStyle().bigText(instruction))
            .setContentIntent(openAt(context, route))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    /**
     * Opens the screen the deadline belongs to — 15 for a transport, 16 for a
     * stay — rather than the generic Today.
     *
     * `SINGLE_TOP` with the launcher semantics the walk and playback
     * notifications already use: the traveller returns to the app they have
     * rather than to a second copy of it stacked on the first.
     */
    fun openAt(context: Context, route: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            route?.let { putExtra(EXTRA_ROUTE, it) }
        }
        return PendingIntent.getActivity(
            context,
            // Distinct per route: a PendingIntent that only differs in its
            // extras is the *same* PendingIntent, and every deadline would
            // open whichever screen was scheduled first.
            route.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Where a target opens, in the navigation graph's own words. */
    fun routeFor(target: AlertTarget): String? = when (target) {
        is AlertTarget.Transport -> Routes.transport(target.transportId)
        is AlertTarget.Stay -> Routes.stay(target.accommodationId)
        // Nothing owns it, so there is no screen more specific than the one
        // the app opens on anyway. A tap returns to Today.
        AlertTarget.Day -> null
    }

    /** Extra carrying the route a notification wants opened. */
    const val EXTRA_ROUTE = "com.travelcompanion.app.extra.ROUTE"
}
