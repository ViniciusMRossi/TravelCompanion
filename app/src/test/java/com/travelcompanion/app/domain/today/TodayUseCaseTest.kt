package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Now/Next selection and timeline state are the rules Today is built on, and
 * they are pure functions of packaged content plus the clock.
 */
class TodayUseCaseTest {

    private val content = packagedContent()
    private val today = TodayUseCase(content)
    private val dayDate: LocalDate = LocalDate.parse(content.days.first().date)

    @Test
    fun theItemUnderWayIsTheOneShownAsNow() {
        val state = today("vinicius", dayDate, LocalTime.of(9, 50))!!

        val now = state.now!!
        assertTrue("an item that has started is active", now.isActive)
        assertEquals("09:40", now.time)
        assertEquals("Baščaršija", now.title)
        assertEquals(TimelineState.Active, state.timeline.first().state)
    }

    @Test
    fun beforeTheDayStartsNowFallsBackToTheNextCommitment() {
        val state = today("vinicius", dayDate, LocalTime.of(7, 0))!!

        val now = state.now!!
        assertFalse("nothing has started yet", now.isActive)
        assertEquals("09:40", now.time)
        assertTrue(state.timeline.all { it.state == TimelineState.Upcoming })
    }

    @Test
    fun anItemOvertakenByTheNextOneStopsBeingNow() {
        val state = today("vinicius", dayDate, LocalTime.of(20, 0))!!

        // 19:30 has started and nothing follows it, so it is what is happening.
        val now = state.now!!
        assertEquals("19:30", now.time)
        assertTrue(now.isActive)
        assertEquals(
            listOf(TimelineState.Past, TimelineState.Past, TimelineState.Active),
            state.timeline.map { it.state },
        )
    }

    @Test
    fun criticalityDoesNotReplaceBookingStatus() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        val bus = state.timeline.single { it.item.startTime == "19:30" }
        assertTrue("the bus is a critical item", bus.isCritical)
        assertEquals("and it stays reserved", BookingStatusUi.Reserved, bus.bookingStatus)
    }

    @Test
    fun theCriticalCardKeepsTheDeadlineSeparateFromTheScheduledTime() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        val bus = state.criticalItems.single { it.item.id == "critical.bus.sarajevo-mostar" }
        assertEquals("19:30", bus.nominalTime)
        assertEquals("19:00", bus.item.actionByTime)
        assertTrue(
            "the deadline is not the scheduled time",
            bus.nominalTime != bus.item.actionByTime,
        )
    }

    @Test
    fun weatherFallsBackToPackagedForecastRatherThanFailing() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        val weather = state.weather as WeatherUi.FallbackFromTrip
        assertEquals(13.0, weather.minC!!, 0.001)
        assertEquals(24.0, weather.maxC!!, 0.001)
    }

    @Test
    fun aDateOutsideTheTripStillRendersADay() {
        val beforeTrip = today("vinicius", LocalDate.parse("2026-01-01"), LocalTime.NOON)
        val afterTrip = today("vinicius", LocalDate.parse("2027-01-01"), LocalTime.NOON)

        assertNotNull("Today must never be empty offline", beforeTrip)
        assertNotNull("Today must never be empty offline", afterTrip)
        assertTrue(beforeTrip!!.timeline.isNotEmpty())
    }

    @Test
    fun otherDaysNeverClaimSomethingIsHappeningNow() {
        val state = today("vinicius", dayDate.plusDays(1), LocalTime.of(9, 50))!!

        assertFalse(state.now!!.isActive)
        assertTrue(state.timeline.none { it.state == TimelineState.Active })
    }

    @Test
    fun theCriticalCardKeepsEveryActionDeclaredForTheItem() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        val bus = state.criticalItems.single { it.item.id == "critical.bus.sarajevo-mostar" }

        // The day declares only "Mostrar passagem"; the transport also declares
        // "Ver ponto de embarque". Losing either strands someone at 19:00.
        assertEquals(
            listOf("Mostrar passagem", "Ver ponto de embarque"),
            bus.item.actionLinks.map { it.label }.sorted(),
        )
        assertNotNull("the ticket action drives the primary button", bus.documentAction)
        assertNotNull("the boarding-point action drives the secondary button", bus.mapsAction)
    }

    @Test
    fun aDocumentIsOnlyLabelledOfflineWhenItsFileIsPackaged() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        val documents = state.shortcuts.filter { it.kind == ShortcutUi.Kind.Document }
        assertTrue("the day carries documents", documents.isNotEmpty())
        assertTrue(
            "no PDF is packaged, so nothing may claim to be offline",
            documents.all { it.trailingNote == null },
        )
    }

    @Test
    fun aDocumentIsLabelledOfflineOnceItsFileIsPackaged() {
        val withBinaries = TodayUseCase(packagedContent(exists = { true }))

        val state = withBinaries("vinicius", dayDate, LocalTime.of(12, 0))!!

        val documents = state.shortcuts.filter { it.kind == ShortcutUi.Kind.Document }
        assertTrue(documents.isNotEmpty())
        assertTrue(documents.all { it.trailingNote == "Offline" })
    }

    @Test
    fun shortcutsCoverDocumentsPlanBAndMemory() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        val kinds = state.shortcuts.map { it.kind }
        assertTrue(kinds.contains(ShortcutUi.Kind.Document))
        assertTrue(kinds.contains(ShortcutUi.Kind.PlanB))
        assertTrue("voice memory is always reachable from Today", kinds.contains(ShortcutUi.Kind.Memory))
    }

    @Test
    fun theDayHeaderReadsInTripLocale() {
        val state = today("vinicius", dayDate, LocalTime.of(12, 0))!!

        assertEquals("Dia 9 de 21", state.dayLabel)
        assertEquals("Segunda, 21 de setembro", state.dateLabel)
        assertEquals("Vinícius", state.participant?.name)
    }
}
