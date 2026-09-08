package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import com.travelcompanion.app.domain.fullday.FullDayUseCase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * The order the day's deadlines arrive in, measured against the real 20 days.
 *
 * `criticalItemsFor` merges in declaration order — the day's own items first,
 * then its transports', then its stays' — and the clock never enters that
 * order. Screen 02 draws the whole list and screen 03 draws
 * `firstOrNull()`, so an unsorted list means screen 03 announces whichever
 * entity happened to be written first (D180).
 *
 * **This is not the finding D156 measured and closed.** That one was about the
 * *timeline* rows of screen 03, which were already sorted by `startTime` and
 * still are — nothing here touches `sectionsOf`, `periodOf` or that sort, and
 * `FullDayTimelineOrderPackagedTest` still guards it. This is a different
 * field: `FullDayUiState.critical`, taken from `criticalItems`, which is a
 * different list and was never sorted. It is also why the two reviews quote
 * different numbers for the same kayak — D156 read `nominalTime` 13:00 and
 * this reads `actionByTime` 12:45, which are both correct and are not the
 * same fact.
 *
 * `assets/trip-production/` is not in the repository (D027, D028), so this
 * asserts nothing when the package is absent; `TodayUseCaseTest` holds the
 * same guarantees on hand-built content and runs on every machine.
 */
class CriticalOrderPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { false }))
    }

    /**
     * Every packaged day hands its deadlines over earliest-limit-first.
     *
     * Stated over all twenty rather than over the three that carry two,
     * because a day that carries one is the case that must not start throwing.
     */
    @Test
    fun `every packaged day yields its deadlines in order of their limit`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val useCase = TodayUseCase(content)
        var withTwo = 0

        content.days.forEach { day ->
            val date = LocalDate.parse(day.date)
            val limits = useCase("vinicius", date, LocalTime.NOON)!!
                .criticalItems
                .mapNotNull { it.item.actionByTime }

            if (limits.size > 1) withTwo++
            assertEquals(
                "day ${day.dayNumber} (${day.date}) hands its limits over in order",
                limits.sorted(),
                limits,
            )
        }

        assertEquals("the package's days carrying more than one deadline", 3, withTwo)
    }

    /**
     * The three days that carry two, named, with the item screen 03 announces.
     *
     * Two of them already agreed before the sort and must go on agreeing —
     * that is the half that says the fix changed what was wrong and left the
     * rest alone. The third is day 16, where merge order put the 12:45 kayak
     * ahead of the 06:30 bus.
     */
    @Test
    fun `the three days with two deadlines announce the earlier one`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val fullDay = FullDayUseCase(content)

        val expected = mapOf(
            // unchanged: merge order already agreed with the clock
            "2026-09-15" to Triple("ci.gate-corfu", "06:45", "ci.checkin-ksamil"),
            "2026-09-17" to Triple("ci.podgorica-conexao", "09:20", "ci.checkin-kotor"),
            // changed: the bus was written on the transport, the kayak on the day
            "2026-09-28" to Triple("ci.bus-dubrovnik", "06:30", "ci.caiaque"),
        )

        expected.forEach { (date, announced) ->
            val (firstId, firstLimit, secondId) = announced
            val day = LocalDate.parse(date)

            val state = fullDay(day, LocalTime.NOON, currentDate = day)
            assertNotNull("screen 03 builds $date", state)

            assertEquals(
                "$date announces the earlier deadline",
                firstId,
                state!!.critical?.item?.id,
            )
            assertEquals("$date announces it by its own limit", firstLimit, state.critical?.item?.actionByTime)
            assertEquals(
                "$date still carries both, in order",
                listOf(firstId, secondId),
                TodayUseCase(content)("vinicius", day, LocalTime.NOON)!!
                    .criticalItems.map { it.item.id },
            )
        }
    }

    /**
     * Day 16, alone, because it is the one the traveller loses something on.
     *
     * The bus from Mostar leaves at 07:00 and is lost at 06:30; the kayak
     * meets at 13:00 and is lost at 12:45. Screen 03 is what gets opened the
     * night before, and before this it opened on the kayak.
     */
    @Test
    fun `day 16 announces the bus that is lost at 06 30, not the kayak`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val day = LocalDate.parse("2026-09-28")
        val critical = FullDayUseCase(content)(day, LocalTime.NOON, currentDate = day)!!.critical!!

        assertEquals("ci.bus-dubrovnik", critical.item.id)
        assertEquals("06:30", critical.item.actionByTime)
        assertEquals("07:00", critical.nominalTime)
    }
}
