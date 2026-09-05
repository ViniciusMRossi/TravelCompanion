package com.travelcompanion.app.domain.fullday

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.today.ShortcutUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** Screen 03 — the same day as Today, read end to end. */
class FullDayUseCaseTest {

    private val content = packagedContent(exists = { false })
    private val useCase = FullDayUseCase(content)
    private val date = LocalDate.parse("2026-09-21")
    private val noon: LocalTime = LocalTime.NOON

    @Test
    fun `the day is split into the parts the approved screen reads`() {
        val sections = useCase(date, noon)!!.sections

        // 09:40 and 11:00 are morning, 19:30 is evening, and the day has
        // nothing in the afternoon — an empty heading is not drawn.
        assertEquals(listOf(DayPeriod.Morning, DayPeriod.Evening), sections.map { it.period })
        assertEquals(2, sections.first().rows.size)
        assertEquals("Ônibus para Mostar", sections.last().rows.single().item.title)
    }

    @Test
    fun `noon and six in the evening are the boundaries`() {
        assertEquals(DayPeriod.Morning, periodOf("11:59"))
        assertEquals(DayPeriod.Afternoon, periodOf("12:00"))
        assertEquals(DayPeriod.Afternoon, periodOf("17:59"))
        assertEquals(DayPeriod.Evening, periodOf("18:00"))
        // Never dropped, whatever the package wrote.
        assertEquals(DayPeriod.Morning, periodOf(null))
        assertEquals(DayPeriod.Morning, periodOf("madrugada"))
    }

    /**
     * The trip runs 2026-09-13 to 2026-10-03 and this package holds one day.
     * There is no day 8 to step back to and no day 10 to step forward to, so
     * both arrows are dead ends rather than clamping onto the same day again.
     */
    /**
     * Found by browsing the real package on a device (D089).
     *
     * Screen 03 reads a *browsed* day, and it asked [TodayUseCase] for it with
     * the browsed date in the argument the "now" guard compares against — so
     * the guard compared the day to itself, always agreed, and marked whatever
     * item the wall clock happened to fall on. Standing on 5 September and
     * paging to the 15th said the 07:15 flight to Corfu was happening.
     *
     * Unreachable with the one-day sample package, where both arrows are null
     * and there is nowhere to browse to. Twenty days is what exposed it.
     */
    @Test
    fun `a browsed day never claims something is happening on it`() {
        val browsed = useCase(date, LocalTime.parse("10:00"), currentDate = LocalDate.parse("2026-09-05"))!!

        val rows = browsed.sections.flatMap { it.rows }
        assertTrue("the package day must have rows to get this wrong with", rows.isNotEmpty())
        assertTrue(
            "no row on a day that is not today may wear the Agora pill",
            rows.none { it.showsNowPill },
        )
    }

    /** The same day, lived rather than browsed, still marks what is under way. */
    @Test
    fun `the day being lived still marks what is under way`() {
        val lived = useCase(date, LocalTime.parse("10:00"), currentDate = date)!!

        val rows = lived.sections.flatMap { it.rows }
        assertTrue(
            "on the real current date the pill is the whole point",
            rows.any { it.showsNowPill },
        )
    }

    @Test
    fun `a single packaged day has nowhere to step to`() {
        val state = useCase(date, noon)!!
        assertNull(state.previousDate)
        assertNull(state.nextDate)
    }

    @Test
    fun `day navigation stops at the ends of the trip`() {
        val threeDays = threeDayContent()
        val useCase = FullDayUseCase(threeDays)

        val first = useCase(LocalDate.parse("2026-09-20"), noon)!!
        assertNull("there is no day 8", first.previousDate)
        assertEquals(LocalDate.parse("2026-09-21"), first.nextDate)

        val middle = useCase(LocalDate.parse("2026-09-21"), noon)!!
        assertEquals(LocalDate.parse("2026-09-20"), middle.previousDate)
        assertEquals(LocalDate.parse("2026-09-22"), middle.nextDate)

        val last = useCase(LocalDate.parse("2026-09-22"), noon)!!
        assertEquals(LocalDate.parse("2026-09-21"), last.previousDate)
        assertNull("there is no day 12", last.nextDate)
    }

    @Test
    fun `the header names the day and the city`() {
        val state = useCase(date, noon)!!
        assertEquals("Dia 9 de 21 · Sarajevo", state.dayLabel)
        assertEquals("Segunda, 21 de setembro", state.dateLabel)
    }

    @Test
    fun `the critical item comes first, and only the next one`() {
        val critical = useCase(date, noon)!!.critical
        assertEquals("Ônibus para Mostar", critical!!.item.title)
        assertEquals("19:30", critical.nominalTime)
    }

    @Test
    fun `the end of the day is the transport and the stay the day declares`() {
        val state = useCase(date, noon)!!

        val transport = state.transports.single()
        assertEquals("19:30", transport.originTime)
        assertEquals("22:00", transport.destinationTime)
        assertEquals("A confirmar", transport.platform)

        assertEquals("Hospedagem em Sarajevo — exemplo", state.stays.single().name)
        assertEquals("14:00", state.stays.single().checkIn)
    }

    /** Recording a memory belongs to Today; this footer is documents and Plan B. */
    @Test
    fun `the footer drops Today's memory shortcut`() {
        val shortcuts = useCase(date, noon)!!.shortcuts

        assertTrue(shortcuts.none { it.kind == ShortcutUi.Kind.Memory })
        assertEquals(
            listOf(ShortcutUi.Kind.Document, ShortcutUi.Kind.Document, ShortcutUi.Kind.PlanB),
            shortcuts.map { it.kind },
        )
    }

    /** The packaged day, repeated at three dates, to give the arrows something. */
    private fun threeDayContent(): TripContent {
        val day = content.days.single()
        val days = listOf(
            day.copy(id = "day08", date = "2026-09-20", dayNumber = 8),
            day,
            day.copy(id = "day10", date = "2026-09-22", dayNumber = 10),
        )
        return TripContent(content.trip.copy(days = days), content.assets)
    }
}
