package com.travelcompanion.app.service.walk

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the process alive and foreground while a walk is running.
 *
 * Android only lets an app hold location while backgrounded if a service of
 * type `location` is in the foreground, and that service must show a
 * notification. That is the whole job here: the walk's decisions live in
 * [WalkModeController], which outlives this service and does not depend on it.
 *
 * The traveller's phone is in a pocket, so the notification says what the walk
 * is doing — which stop they are on — rather than that an app is running.
 */
class WalkModeService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android refuses a foreground service of type `location` outright
        // when the app holds no location permission, and the refusal is a
        // SecurityException on this thread. It can arrive legitimately — the
        // permission can be revoked between the walk starting and this call —
        // so it is checked here rather than trusted from the caller.
        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent?.getStringExtra(EXTRA_TEXT).orEmpty()

        WalkNotifications.ensureChannels(this)
        val notification = WalkNotifications.walkNotification(this, title, text)

        ServiceCompat.startForeground(
            this,
            WalkNotifications.WALK_NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )

        // The walk is driven from the controller, not from redelivered
        // intents: restarting this service must never restart a walk the
        // traveller already ended.
        return START_NOT_STICKY
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TEXT = "text"

        fun intent(context: Context, title: String, text: String): Intent =
            Intent(context, WalkModeService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text)
    }
}
