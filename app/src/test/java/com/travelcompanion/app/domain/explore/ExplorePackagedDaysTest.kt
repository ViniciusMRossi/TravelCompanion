package com.travelcompanion.app.domain.explore

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import com.travelcompanion.app.domain.food.cityOfDay
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * The six days of the real trip, asserted against the real package.
 *
 * `ExploreCitiesTest` builds its shapes by hand and is the test that must
 * always run. This one reads `assets/trip-production/`, which is **not in the
 * repository** (D027, D028) — the operational package carries booking
 * locators and door PINs. So it asserts nothing when the package is absent,
 * and everything when it is there, which is every build made on the machine
 * the trip is packaged on.
 *
 * It exists because the hand-built shapes are a reading of the package, and a
 * reading can be wrong about the package it claims to describe.
 */
class ExplorePackagedDaysTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { false }))
    }

    /** dayNumber to (chips drawn, city opened on). */
    private val expected = listOf(
        // No city of day 1 packages anything: the floor, and no band.
        Triple(1, 0, "sao-paulo"),
        // Amsterdam is behind by the time the day lands; every row is a leg,
        // so the base decides.
        Triple(3, 2, "ksamil"),
        // One city with content: no band at all.
        Triple(9, 0, "zabljak"),
        // 09:00, the rafting — the day is Bastasi's, the bed is Sarajevo's.
        Triple(12, 2, "bastasi"),
        // 11:00, the first parada of the Herzegovina circuit.
        Triple(15, 4, "blagaj"),
        // 08:00, the walls — and the base, Cilipi, packages nothing.
        Triple(17, 2, "dubrovnik"),
    )

    @Test
    fun `the six days resolve the way the package says they should`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        expected.forEach { (dayNumber, chipCount, opensAt) ->
            val day = content.days.single { it.dayNumber == dayNumber }
            val explore = exploreCitiesOf(content, day)

            assertEquals("day $dayNumber chips", chipCount, explore.chips.size)
            assertEquals("day $dayNumber opens at", opensAt, explore.openAtCityId)
        }
    }

    /** Every day of the trip: dayNumber to (chips drawn, city opened on). */
    private val wholeTrip = listOf(
        Triple(1, 0, "sao-paulo"), Triple(2, 0, "amsterdam"), Triple(3, 2, "ksamil"),
        Triple(4, 2, "butrinto"), Triple(5, 2, "kotor"), Triple(6, 0, "kotor"),
        Triple(7, 0, "kotor"), Triple(8, 2, "zabljak"), Triple(9, 0, "zabljak"),
        Triple(10, 0, "zabljak"), Triple(11, 2, "bastasi"), Triple(12, 2, "bastasi"),
        Triple(13, 2, "sarajevo"), Triple(14, 2, "mostar"), Triple(15, 4, "blagaj"),
        Triple(16, 2, "dubrovnik"), Triple(17, 2, "dubrovnik"), Triple(18, 2, "amsterdam"),
        Triple(19, 0, "amsterdam"), Triple(20, 0, "amsterdam"),
    )

    @Test
    fun `all twenty days, chips and opening city`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val actual = content.days.map { day ->
            val explore = exploreCitiesOf(content, day)
            Triple(day.dayNumber, explore.chips.size, explore.openAtCityId)
        }

        assertEquals(wholeTrip, actual)
    }

    @Test
    fun `twelve of the twenty days carry more than one city with content`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val banded = content.days.count { exploreCitiesOf(content, it).chips.isNotEmpty() }

        assertEquals(12, banded)
        assertEquals(20, content.days.size)
    }

    @Test
    fun `Explorar leaves the base on four days, and the other screens do not`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val moved = content.days.filter { day ->
            exploreCitiesOf(content, day).openAtCityId != cityOfDay(content, day)?.id
        }

        // Four days sleep somewhere other than where they are spent: day 4 in
        // Ksamil for Butrinto, day 12 in Sarajevo for the Tara, day 15 in
        // Mostar for Blagaj, day 17 in Cilipi for Dubrovnik. Those four are
        // the whole defect — the other sixteen already opened correctly, and
        // this change must not move them.
        assertEquals(
            listOf(4, 12, 15, 17),
            moved.map { it.dayNumber },
        )

        // And on every one of them the base is still the base: nothing here
        // changed what screens 02, 03, 17 and 20 read.
        moved.forEach { day ->
            assertEquals(
                "day ${day.dayNumber} base",
                day.baseCityId,
                cityOfDay(content, day)?.id,
            )
        }
    }
}
