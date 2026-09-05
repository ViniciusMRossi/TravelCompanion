package com.travelcompanion.app.service.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Posts one deadline when its alarm fires. */
class CriticalAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        // No sentence, no notification — the same rule the scheduler applies
        // before an alarm is ever set, held again at the last moment.
        val instruction = intent.getStringExtra(EXTRA_INSTRUCTION)?.takeIf { it.isNotBlank() }
            ?: return

        // Checked inline rather than behind a helper: `notify` throws without
        // it, inside a receiver, where a crash is the system's to report and
        // the traveller's to see — and lint only recognises the guard when it
        // can see it here. A refused permission is a decision, not an error.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        OperationalNotifications.ensureChannels(context)
        NotificationManagerCompat.from(context).notify(
            OperationalNotifications.notificationId(id),
            OperationalNotifications.deadlineNotification(
                context = context,
                title = title,
                instruction = instruction,
                route = intent.getStringExtra(EXTRA_ROUTE),
            ),
        )
    }

    companion object {
        const val EXTRA_ID = "com.travelcompanion.app.extra.ALERT_ID"
        const val EXTRA_TITLE = "com.travelcompanion.app.extra.ALERT_TITLE"
        const val EXTRA_INSTRUCTION = "com.travelcompanion.app.extra.ALERT_INSTRUCTION"
        const val EXTRA_ROUTE = "com.travelcompanion.app.extra.ALERT_ROUTE"
    }
}
