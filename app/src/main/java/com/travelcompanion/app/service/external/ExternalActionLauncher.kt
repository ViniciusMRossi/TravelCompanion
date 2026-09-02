package com.travelcompanion.app.service.external

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens specialist apps contextually and always comes back here.
 *
 * External apps stay external: Travel Companion never reimplements maps,
 * dialer or browser. Every launch has a fallback so a missing app is not a
 * dead end on the road.
 */
class ExternalActionLauncher(private val context: Context) {

    fun open(uri: String, fallbackUri: String? = null): Boolean {
        if (launch(uri)) return true
        if (fallbackUri != null && launch(fallbackUri)) return true
        return false
    }

    private fun launch(uri: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
