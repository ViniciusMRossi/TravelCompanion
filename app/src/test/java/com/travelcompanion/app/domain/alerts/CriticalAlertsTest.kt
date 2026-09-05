package com.travelcompanion.app.domain.alerts

import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TimelineItem
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Which deadlines ring, and at what instant (D092).
 *
 * This is the first thing in the app that **computes** with `day.timeZone`
 * rather than printing it: until now the zone decided what a screen writes,
 * and here it decides when a phone rings. An hour out is invisible on every
 * screen and only wrong at the gate, so the conversion is tested rather than
 * eyeballed.
 */
class CriticalAlertsTest {

    private val content = packagedContent(exists = { false })
    private val longAgo: Instant = Instant.parse("2020-01-01T00:00:00Z")

    // -- the conversion --------------------------------------------------

    /**
     * A deadline is a wall clock in the zone the package wrote it in, and
     * becomes an instant only there.
     *
     * Amsterdam in September is +02:00, so 06:45 local is 04:45 UTC. Read as
     * UTC — the shape of the bug this test exists for — it would be 06:45 UTC
     * and the phone would ring two hours after the gate closed.
     */
    @Test
    fun `a deadline is an instant in the day's own zone`() {
        val day = dayAt("Europe/Amsterdam", "2026-09-15", actionBy = "06:45")

        val alert = criticalAlerts(day, longAgo).single()

        assertEquals(Instant.parse("2026-09-15T04:45:00Z"), alert.at)
        assertEquals(ZoneId.of("Europe/Amsterdam"), alert.zone)
    }

    /**
     * Day 3's shape: the day is Amsterdam, and the items that happen after the
     * ferry declare Athens and Tirane. A critical item hanging off one of
     * those inherits the item's zone, not the day's.
     *
     * Athens is +03:00 while the day is +02:00, so this is the one hour that
     * separates a right alarm from a wrong one, inside a single day.
     */
    @Test
    fun `an item that declares its own zone is read in that zone, not the day's`() {
        val day = dayAt("Europe/Amsterdam", "2026-09-15", actionBy = "13:45", itemZone = "Europe/Athens")

        val alert = criticalAlerts(day, longAgo).single()

        assertEquals(ZoneId.of("Europe/Athens"), alert.zone)
        assertEquals(Instant.parse("2026-09-15T10:45:00Z"), alert.at)
        // The same wall clock read as the day's zone would be an hour later.
        assertTrue(alert.at < Instant.parse("2026-09-15T11:45:00Z"))
    }

    /**
     * Day 20's shape: an Amsterdam day whose last item happens in São Paulo,
     * five hours behind. Nothing in the real package puts a deadline there,
     * but the conversion has to be right if one ever arrives.
     */
    @Test
    fun `the day 20 crossing is five hours, and the zone decides which side`() {
        val amsterdam = criticalAlerts(
            dayAt("Europe/Amsterdam", "2026-10-02", actionBy = "21:00"),
            longAgo,
        ).single()
        val saoPaulo = criticalAlerts(
            dayAt("Europe/Amsterdam", "2026-10-02", actionBy = "21:00", itemZone = "America/Sao_Paulo"),
            longAgo,
        ).single()

        assertEquals(Instant.parse("2026-10-02T19:00:00Z"), amsterdam.at)
        assertEquals(Instant.parse("2026-10-03T00:00:00Z"), saoPaulo.at)
        assertEquals(
            5 * 60L,
            java.time.Duration.between(amsterdam.at, saoPaulo.at).toMinutes(),
        )
    }

    // -- what does not ring ----------------------------------------------

    @Test
    fun `an item with no actionByTime is not a deadline`() {
        assertTrue(criticalAlerts(dayAt("Europe/Amsterdam", "2026-09-15", actionBy = null), longAgo).isEmpty())
    }

    /** The body is the packaged sentence. With none, there is nothing to say. */
    @Test
    fun `an item with no instruction is never announced`() {
        val day = dayAt("Europe/Amsterdam", "2026-09-15", actionBy = "06:45", instruction = "  ")

        assertTrue(criticalAlerts(day, longAgo).isEmpty())
    }

    @Test
    fun `a deadline that has gone is not scheduled`() {
        val day = dayAt("Europe/Amsterdam", "2026-09-15", actionBy = "06:45")

        assertTrue(criticalAlerts(day, Instant.parse("2026-09-15T04:45:00Z")).isEmpty())
        assertEquals(1, criticalAlerts(day, Instant.parse("2026-09-15T04:44:59Z")).size)
    }

    /**
     * An accommodation's critical item is pulled into every day of the stay,
     * so Kotor's "check-in fecha às 21:00" reads on three days. It is one
     * deadline, on the night the traveller arrives.
     */
    @Test
    fun `a stay spanning nights rings once, on the first`() {
        val alerts = criticalAlerts(threeNightStay(), longAgo)

        assertEquals(1, alerts.size)
        assertEquals(Instant.parse("2026-09-17T18:00:00Z"), alerts.single().at)
        assertEquals(AlertTarget.Stay("acc.kotor"), alerts.single().target)
    }

    // -- the real package ------------------------------------------------

    /**
     * The packaged trip, end to end. The count and the ordering are the part
     * a device cannot show: a scheduler is only as right as the list it is
     * handed.
     */
    @Test
    fun `the packaged trip yields deadlines in order, each in its own zone`() {
        val alerts = criticalAlerts(content, longAgo)

        assertTrue("the package must declare deadlines to test with", alerts.isNotEmpty())
        assertEquals(
            "one alert per critical item id",
            alerts.map { it.criticalItemId }.distinct().size,
            alerts.size,
        )
        assertEquals(alerts.sortedBy { it.at }, alerts)
        alerts.forEach {
            assertTrue("every alert carries the packaged sentence", it.instruction.isNotBlank())
            assertTrue("every alert carries a title", it.title.isNotBlank())
        }
    }

    @Test
    fun `nothing before the cutoff survives it`() {
        val all = criticalAlerts(content, longAgo)
        val cutoff = all.first().at

        assertTrue(criticalAlerts(content, cutoff).none { it.at <= cutoff })
        assertEquals(all.size - 1, criticalAlerts(content, cutoff).size)
    }

    // -- fixtures ---------------------------------------------------------

    /** One day, one critical item, hung off one timeline item. */
    private fun dayAt(
        dayZone: String,
        date: String,
        actionBy: String?,
        itemZone: String? = null,
        instruction: String = "Esteja no portão até 06:45.",
    ): TripContent {
        val critical = CriticalItem(
            id = "ci.test",
            title = "Portão fecha",
            nominalTime = "07:15",
            actionByTime = actionBy,
            instruction = instruction,
        )
        val day = TripDay(
            id = "day01",
            date = date,
            dayNumber = 1,
            timeZone = dayZone,
            cityIds = listOf("sarajevo"),
            timeline = listOf(
                TimelineItem(
                    id = "t1",
                    startTime = "07:15",
                    kind = "transport",
                    title = "Voo",
                    timeZone = itemZone,
                    criticalItemIds = listOf("ci.test"),
                ),
            ),
            criticalItems = listOf(critical),
        )
        return TripContent(content.trip.copy(days = listOf(day)), content.assets)
    }

    /** The Kotor shape: one accommodation declared on three consecutive days. */
    private fun threeNightStay(): TripContent {
        val stay = content.trip.accommodations.first().copy(
            id = "acc.kotor",
            criticalItems = listOf(
                CriticalItem(
                    id = "ci.checkin-kotor",
                    title = "Check-in fecha às 21:00",
                    actionByTime = "20:00",
                    instruction = "Saia de Budva a tempo de chegar entre 19:00 e 20:00.",
                ),
            ),
        )
        val days = listOf("2026-09-17", "2026-09-18", "2026-09-19").mapIndexed { index, date ->
            TripDay(
                id = "day${index + 1}",
                date = date,
                dayNumber = index + 1,
                timeZone = "Europe/Podgorica",
                cityIds = listOf("sarajevo"),
                accommodationIds = listOf("acc.kotor"),
            )
        }
        return TripContent(
            content.trip.copy(days = days, accommodations = listOf(stay)),
            content.assets,
        )
    }
}
