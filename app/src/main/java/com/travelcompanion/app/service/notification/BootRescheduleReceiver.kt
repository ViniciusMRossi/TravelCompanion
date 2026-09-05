package com.travelcompanion.app.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.travelcompanion.app.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Puts the deadlines back after a restart.
 *
 * Android drops every alarm on boot, and a twenty-day trip goes through
 * reboots — a phone that runs out of battery in Žabljak and is charged
 * overnight would otherwise arrive in Sarajevo announcing nothing.
 */
class BootRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULING_ACTIONS) return

        val scheduler = context.appContainer.criticalAlertScheduler
        // The work outlives this receiver's ten seconds only in the sense that
        // it must not be done on its thread; reading one packaged file and
        // registering twelve alarms is short.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                scheduler.reschedule()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULING_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            // Sent to the app that was just updated: an install replaces the
            // process and clears nothing, but the content may have changed.
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Android 12+ tells an app when its exact-alarm permission is
            // granted or revoked. Granting it should upgrade the alarms that
            // were registered inexactly, without waiting for a launch (D093).
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
