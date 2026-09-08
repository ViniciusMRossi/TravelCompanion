package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import com.travelcompanion.app.domain.explore.exploreCitiesOf
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * The twenty days of the real trip, and the coordinate each of them asks the
 * weather about.
 *
 * `DayWeatherTest` builds its shapes by hand and is the test that must always
 * run. This one reads `assets/trip-production/`, which is **not in the
 * repository** (D027, D028), so it asserts nothing when the package is absent
 * and everything when it is there — the same bargain `ExplorePackagedDaysTest`
 * makes, and for the same reason: the hand-built shapes are a *reading* of the
 * package, and a reading can be wrong about what it claims to describe.
 */
class DayWeatherPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { false }))
    }

    /**
     * Nineteen of the twenty days have somewhere to ask about.
     *
     * The one that does not is day 1: São Paulo packages no attraction, so
     * there is no coordinate, so there is no call — and the day falls straight
     * to its packaged forecast, which is the right answer for a day that
     * starts at home and ends on a night flight.
     */
    @Test
    fun `nineteen of the twenty days yield a coordinate, and day one does not`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val withPoint = content.days.filter { dayWeatherRequestOf(content, it) != null }

        assertEquals(20, content.days.size)
        assertEquals(19, withPoint.size)
        assertEquals(listOf(1), (content.days - withPoint.toSet()).map { it.dayNumber })
        assertNull(
            dayWeatherRequestOf(content, content.days.single { it.dayNumber == 1 }),
        )
    }

    /** The city asked about is the one Explorar opens on, on every single day. */
    @Test
    fun `the coordinate belongs to the city the day happens in`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        content.days.forEach { day ->
            val request = dayWeatherRequestOf(content, day) ?: return@forEach
            val city = content.city(exploreCitiesOf(content, day).openAtCityId)!!
            val points = city.attractionIds.mapNotNull { content.attraction(it)?.location?.geo }

            assertEquals("day ${day.dayNumber} zone", day.timeZone, request.timeZone)
            // A mean lies inside the bounding box of what it averages. This is
            // what tells a centroid apart from a coordinate picked elsewhere.
            assertTrue(
                "day ${day.dayNumber} latitude is outside ${city.id}",
                request.point.latitude >= points.minOf { it.latitude } - TOLERANCE &&
                    request.point.latitude <= points.maxOf { it.latitude } + TOLERANCE,
            )
            assertTrue(
                "day ${day.dayNumber} longitude is outside ${city.id}",
                request.point.longitude >= points.minOf { it.longitude } - TOLERANCE &&
                    request.point.longitude <= points.maxOf { it.longitude } + TOLERANCE,
            )
        }
    }

    /**
     * And it is the mean, not the first element.
     *
     * Sixteen of the nineteen days average more than one attraction; on every
     * one of them the coordinate differs from the city's first, which is the
     * assertion that would fail the day somebody writes `attractionIds.first()`
     * because it is shorter (D097).
     */
    @Test
    fun `no day asks about its first attraction`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val averaged = content.days.mapNotNull { day ->
            val request = dayWeatherRequestOf(content, day) ?: return@mapNotNull null
            val city = content.city(exploreCitiesOf(content, day).openAtCityId)!!
            val first = content.attraction(city.attractionIds.first())?.location?.geo
                ?: return@mapNotNull null
            if (city.attractionIds.size < 2) return@mapNotNull null
            assertTrue(
                "day ${day.dayNumber} is reading ${city.id}'s first attraction",
                abs(first.latitude - request.point.latitude) > TOLERANCE ||
                    abs(first.longitude - request.point.longitude) > TOLERANCE,
            )
            day.dayNumber
        }

        assertEquals(16, averaged.size)
    }

    /**
     * The four days the trip spends in Dubrovnik and Kotor ask about one point
     * each, not four. Two days in one city are one forecast, and the cache is
     * allowed to say so.
     */
    @Test
    fun `days spent in the same city ask about the same point`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        fun pointOf(dayNumber: Int) =
            dayWeatherRequestOf(content, content.days.single { it.dayNumber == dayNumber })!!.point

        assertEquals(pointOf(16), pointOf(17))
        assertEquals(pointOf(5), pointOf(6))
        assertEquals(pointOf(6), pointOf(7))
        // ... and Kotor is not Dubrovnik, which is the whole of D089's worry.
        assertNotEquals(pointOf(5), pointOf(16))
    }

    private companion object {
        /** Four decimals is what the coordinate is rounded to; about 11 metres. */
        const val TOLERANCE = 0.0001
    }
}
