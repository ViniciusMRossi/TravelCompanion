package com.travelcompanion.app.service.notification

import com.travelcompanion.app.domain.alerts.AlertTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where a deadline opens, and that two deadlines do not collide (D092).
 */
class OperationalNotificationsTest {

    /** A transport deadline opens screen 15, a stay's opens screen 16. */
    @Test
    fun `a deadline opens the screen its thing lives on`() {
        assertEquals(
            "transport/transport.ams-corfu.u2",
            OperationalNotifications.routeFor(AlertTarget.Transport("transport.ams-corfu.u2")),
        )
        assertEquals(
            "stay/acc.kotor.suranj",
            OperationalNotifications.routeFor(AlertTarget.Stay("acc.kotor.suranj")),
        )
    }

    /**
     * A deadline the day declares owns no screen more specific than the one
     * the app opens on anyway, so it asks for none.
     */
    @Test
    fun `a deadline with no owner asks for no screen`() {
        assertNull(OperationalNotifications.routeFor(AlertTarget.Day))
    }

    /**
     * Ids are the alarm's request code and the notification's id at once. Two
     * deadlines sharing one would mean the second alarm replacing the first,
     * and the traveller hearing about the gate but never about the bag drop.
     */
    @Test
    fun `the twelve packaged deadlines do not collide on an id`() {
        val ids = listOf(
            "ci.anne-frank", "ci.gate-corfu", "ci.checkin-ksamil", "ci.nightbus-ksamil",
            "ci.checkin-kotor", "ci.bus-zabljak", "ci.canyoning", "ci.train-mostar",
            "ci.tour-herzegovina", "ci.bus-dubrovnik", "ci.caiaque", "ci.bagdrop-dubrovnik",
        ).map(OperationalNotifications::notificationId)

        assertEquals(ids.size, ids.distinct().size)
    }

    /** And they must not land on the walk's or a story's notification either. */
    @Test
    fun `deadline ids stay out of the walk and story ranges`() {
        val id = OperationalNotifications.notificationId("ci.gate-corfu")

        assertNotEquals(2001, id)
        assert(id in 4000..4095 + 4000) { "deadline ids live in their own block" }
    }
}
