package com.travelcompanion.app.service.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import com.travelcompanion.app.domain.alerts.CriticalAlert
import com.travelcompanion.app.domain.alerts.criticalAlerts
import com.travelcompanion.app.data.trip.TripRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Puts the decided list of deadlines into `AlarmManager`, and nothing else.
 *
 * The deciding happened in `domain/alerts`, with no Android in it. What is
 * left here is the part that genuinely needs the framework: registering the
 * instants, surviving a reboot, and replacing the set when the content or the
 * traveller changes.
 *
 * No new dependency: `AlarmManager` is the framework's, and `WorkManager`
 * would be a library for a job that is a single one-shot per deadline.
 */
class CriticalAlertScheduler(
    context: Context,
    private val tripRepository: TripRepository,
    private val now: () -> Instant = Instant::now,
) {
    private val appContext = context.applicationContext
    private val alarms = appContext.getSystemService(AlarmManager::class.java)

    /**
     * Whether this build can ask for an alarm that fires at the minute.
     *
     * `SCHEDULE_EXACT_ALARM` is declared and, before Android 13, granted on
     * install. From 13 it starts denied — observed on the S24, where the
     * first build registered every alarm with a one-hour window — and is only
     * grantable through Android's own settings screen, which the app opens
     * once and never again (D093).
     */
    fun canBeExact(): Boolean = when {
        alarms == null -> false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> alarms.canScheduleExactAlarms()
        else -> true
    }

    /**
     * Whether the traveller has already been shown the system screen that
     * grants exact alarms.
     *
     * Asked once, ever. A deadline is worth one interruption; a settings
     * screen that reappears on every launch is the app nagging, and the
     * answer "no" is a decision this app respects the way it respects a
     * refused microphone (D093).
     */
    fun shouldAskForExact(): Boolean =
        !canBeExact() && !store().getBoolean(KEY_ASKED_EXACT, false)

    fun markExactAsked() {
        store().edit { putBoolean(KEY_ASKED_EXACT, true) }
    }

    /**
     * Replaces every scheduled deadline with the ones still ahead.
     *
     * Called after content loads, after the traveller changes, and after boot.
     * Cheap enough to be unconditional: twelve alarms is the whole trip.
     */
    suspend fun reschedule() {
        val content = runCatching { tripRepository.load() }.getOrNull() ?: return
        OperationalNotifications.ensureChannels(appContext)
        schedule(criticalAlerts(content, now()))
    }

    fun schedule(alerts: List<CriticalAlert>) {
        val manager = alarms ?: return
        // Cancel first: an alert whose instant moved would otherwise keep the
        // old alarm as well as gaining a new one, and the traveller would be
        // told twice, once wrongly.
        cancelAll()

        // Asked once rather than per alarm, so the line logged for each alert
        // names the mode that alert was actually registered with (D111).
        val exact = canBeExact()
        alerts.forEach { alert ->
            val fire = alert.at.toEpochMilli()
            val operation = firePendingIntent(alert)
            if (exact) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, operation)
            } else {
                // Degraded rather than absent, for a traveller who refused
                // the settings screen or is on a build that cannot grant it:
                // the system may hold this until its next maintenance window,
                // which for a deadline is the defect — but a late warning
                // still beats none (D093).
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, operation)
            }
            Log.i(
                TAG,
                "alarm scheduled id=${alert.criticalItemId} " +
                    "at=${localIso(fire)} mode=${if (exact) "exact" else "inexact"}",
            )
            remember(alert)
        }
        Log.i(TAG, "alarms scheduled total=${alerts.size} canBeExact=$exact")
    }

    /**
     * The fire instant in the phone's own zone, which is the only rendering
     * that answers "will this ring at 04:45 where I am standing" (D111).
     */
    private fun localIso(epochMillis: Long): String =
        DateTimeFormatter.ISO_ZONED_DATE_TIME.format(
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()),
        )

    private fun cancelAll() {
        val manager = alarms ?: return
        scheduledIds().forEach { id ->
            manager.cancel(
                PendingIntent.getBroadcast(
                    appContext,
                    requestCode(id),
                    Intent(appContext, CriticalAlertReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
                ) ?: return@forEach,
            )
        }
        store().edit { remove(KEY_IDS) }
    }

    private fun firePendingIntent(alert: CriticalAlert): PendingIntent {
        val intent = Intent(appContext, CriticalAlertReceiver::class.java).apply {
            putExtra(CriticalAlertReceiver.EXTRA_ID, alert.criticalItemId)
            putExtra(CriticalAlertReceiver.EXTRA_TITLE, alert.title)
            putExtra(CriticalAlertReceiver.EXTRA_INSTRUCTION, alert.instruction)
            putExtra(
                CriticalAlertReceiver.EXTRA_ROUTE,
                OperationalNotifications.routeFor(alert.target),
            )
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(alert.criticalItemId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /**
     * Which ids have alarms, so they can be cancelled later.
     *
     * `AlarmManager` cannot be asked what it holds, and a trip whose content
     * changes would otherwise leave alarms nobody can reach.
     */
    private fun remember(alert: CriticalAlert) {
        val ids = scheduledIds() + alert.criticalItemId
        store().edit { putStringSet(KEY_IDS, ids) }
    }

    private fun scheduledIds(): Set<String> =
        store().getStringSet(KEY_IDS, emptySet()).orEmpty()

    private fun store() = appContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    private fun requestCode(criticalItemId: String): Int =
        OperationalNotifications.notificationId(criticalItemId)

    private companion object {
        /** One tag for the whole app, so `adb logcat -s TravelCompanion` is the filter. */
        const val TAG = "TravelCompanion"
        const val STORE = "critical_alerts"
        const val KEY_IDS = "scheduled_ids"
        const val KEY_ASKED_EXACT = "asked_exact_alarm"
    }
}
