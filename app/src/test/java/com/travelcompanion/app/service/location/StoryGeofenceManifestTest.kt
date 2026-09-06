package com.travelcompanion.app.service.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The two manifest facts passive discovery cannot work without, and which no
 * other test in the repository would notice going missing.
 *
 * `ACCESS_BACKGROUND_LOCATION` was declared once and removed for being unused
 * (D033); it is back because it is used now, and a build that carries the
 * geofencing code without the declaration registers nothing, silently, on
 * every device. The receiver is the other half: a transition delivered to a
 * component the manifest does not name goes nowhere, also silently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoryGeofenceManifestTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `the background location permission is declared`() {
        val declared = app.packageManager
            .getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toList()

        assertTrue(
            "ACCESS_BACKGROUND_LOCATION is not declared; geofences would never register",
            Manifest.permission.ACCESS_BACKGROUND_LOCATION in declared,
        )
        // Geofencing needs the fine grant as well; the coarse one alone is
        // refused from Android 10.
        assertTrue(Manifest.permission.ACCESS_FINE_LOCATION in declared)
    }

    @Test
    fun `the transition receiver is declared and is not exported`() {
        val receivers = app.packageManager
            .getPackageInfo(app.packageName, PackageManager.GET_RECEIVERS)
            .receivers
            .orEmpty()

        val receiver = receivers.firstOrNull {
            it.name == StoryGeofenceReceiver::class.java.name
        }
        assertNotNull("StoryGeofenceReceiver is not in the manifest", receiver)
        // The PendingIntent that carries a transition is this app's own, so
        // nothing outside it has any business waking a story.
        assertFalse(receiver!!.exported)
    }
}
