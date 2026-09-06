package com.travelcompanion.app.service.walk

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.travelcompanion.app.feature.shell.Routes
import com.travelcompanion.app.service.notification.OperationalNotifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Where a story notification lands, which is the one thing that differs
 * between the walk's and passive discovery's (D105 (a)).
 *
 * Screen 10 is a sheet over screen 07, so during a walk a tap returns to the
 * walk the traveller already has and the sheet is already there. Outside a
 * walk there is no screen underneath, and the approved screen that carries the
 * same story with its own *Ouvir* and *Ler* is screen 04 — so the passive
 * notification asks for that route, through the extra `MainActivity` already
 * reads for a deadline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoryNotificationRouteTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun routeOf(route: String?): String? {
        val notification = WalkNotifications.storyNotification(app, "Título", "Gancho", route)
        return shadowOf(notification.contentIntent)
            .savedIntent
            .getStringExtra(OperationalNotifications.EXTRA_ROUTE)
    }

    @Test
    fun `a story found outside a walk opens the city screen`() {
        assertEquals("explore", Routes.EXPLORE)
        assertEquals(Routes.EXPLORE, routeOf(Routes.EXPLORE))
    }

    @Test
    fun `a story found during a walk asks for no screen`() {
        assertNull(routeOf(null))
    }
}
