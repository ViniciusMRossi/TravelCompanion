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

    /**
     * Opens the dialer with a number filled in, and stops there.
     *
     * `ACTION_DIAL`, never `ACTION_CALL`: dialling immediately would need the
     * `CALL_PHONE` permission, and this repository has just finished removing
     * a permission it declared and never used. The traveller presses the call
     * button themselves, which is also the right shape for a screen whose
     * numbers include emergency services (D064).
     */
    fun dial(number: String): Boolean = try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
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
