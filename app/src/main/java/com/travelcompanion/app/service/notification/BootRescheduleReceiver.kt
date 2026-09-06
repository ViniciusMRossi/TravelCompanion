package com.travelcompanion.app.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.travelcompanion.app.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Puts the deadlines and the story circles back after a restart.
 *
 * Android drops every alarm on boot, and a twenty-day trip goes through
 * reboots — a phone that runs out of battery in Žabljak and is charged
 * overnight would otherwise arrive in Sarajevo announcing nothing.
 *
 * Geofences are dropped on boot too, and by exactly the same rule. They are
 * put back here rather than from a second boot receiver: one broadcast, one
 * receiver, one place that knows what a restart costs (D103).
 */
class BootRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULING_ACTIONS) return

        val container = context.appContainer
        // The work outlives this receiver's ten seconds only in the sense that
        // it must not be done on its thread; reading one packaged file, then
        // registering twelve alarms and three circles, is short.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                container.criticalAlertScheduler.reschedule()
                // Registers only what has not already spoken, and does nothing
                // at all without the background-location permission.
                container.passiveStoryDiscovery.refresh()
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
