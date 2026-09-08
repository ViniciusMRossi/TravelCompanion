package com.travelcompanion.app.domain.fullday

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * **This test was born green, and that is the whole point of it.**
 *
 * A review recorded that screen 03 hid day 16's 06:30 bus behind the 13:00
 * kayak. It does not, and it never did: `TodayUseCase` sorts the timeline by
 * `startTime` before `FullDayUseCase` reads it, `sectionsOf` groups by the
 * clock and `filter` preserves order inside each period, all 61 rows of the
 * 20 packaged days are already written in ascending order, and not one of
 * them omits `startTime`. The finding was measured and did not exist (D156).
 *
 * So this asserts an order that is already correct, and it is here as a guard
 * against the *repair* rather than against the defect: the cheapest way for
 * day 16 to actually break is for someone to come back to that review, take
 * it at its word, and re-sort a list that is already sorted. If that happens,
 * this goes red.
 *
 * Nothing in `sectionsOf`, `periodOf` or the sort was touched.
 */
class FullDayTimelineOrderPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { false }))
    }

    @Test
    fun `every row of every packaged day leaves the use case in clock order`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val useCase = FullDayUseCase(content)
        var rows = 0

        content.days.forEach { day ->
            val date = LocalDate.parse(day.date)
            val times = useCase(date, LocalTime.NOON, currentDate = date)!!
                .sections
                .flatMap { section -> section.rows }
                .map { it.item.startTime }
            rows += times.size

            assertEquals("day ${day.dayNumber} drops no row", day.timeline.size, times.size)
            assertEquals("day ${day.dayNumber} is out of order", times.sorted(), times)
        }

        assertEquals("the whole trip, row for row", 61, rows)
        assertEquals(20, content.days.size)
    }

    /** The day the review named, hour by hour. */
    @Test
    fun `day 16 reads 0630, 0700, 1100, 1300 and 1600, in that order`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val day = content.days.single { it.dayNumber == 16 }
        val date = LocalDate.parse(day.date)
        val useCase = FullDayUseCase(content)

        val times = useCase(date, LocalTime.NOON, currentDate = date)!!
            .sections
            .flatMap { section -> section.rows }
            .map { it.item.startTime }

        assertEquals(listOf("06:30", "07:00", "11:00", "13:00", "16:00"), times)
    }
}
