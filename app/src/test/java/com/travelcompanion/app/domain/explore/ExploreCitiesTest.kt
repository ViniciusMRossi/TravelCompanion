package com.travelcompanion.app.domain.explore

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.City
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TimelineItem
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which city Explorar opens on, and which cities it offers.
 *
 * Screen 04 asks a different question from screens 02, 03, 17 and 20: not
 * *where does the traveller sleep* but *where does the day happen*. The base
 * answers the first and cannot answer the second — day 17 sleeps in Čilipi,
 * which packages nothing, and spends itself in Dubrovnik and its thirteen
 * attractions.
 *
 * These cases are built by hand rather than read from the real package,
 * because the real package is not in the repository (D027, D028) and a test
 * that needs it cannot run on a fresh clone. The shapes are the real ones —
 * see `ExplorePackagedDaysTest` for the same six days asserted against the
 * package itself when it happens to be present.
 */
class ExploreCitiesTest {

    /**
     * A trip whose cities are exactly the ids given, each with one attraction
     * unless it is named in [empty] — those package nothing at all, the way
     * Čilipi, Corfu and Zagreb do.
     */
    private fun content(vararg cityIds: String, empty: Set<String> = emptySet()): TripContent {
        val packaged = packagedContent()
        val template = packaged.trip.cities.single()
        val attraction = packaged.trip.attractions.first()

        val cities = cityIds.map { id ->
            template.copy(
                id = id,
                name = id.replaceFirstChar(Char::uppercase),
                attractionIds = if (id in empty) emptyList() else listOf("attr.$id"),
                walkIds = emptyList(),
                storyIds = emptyList(),
                restaurants = emptyList(),
                audioGuideId = null,
                menu = null,
            )
        }
        val attractions = cities
            .filter { it.attractionIds.isNotEmpty() }
            .map { city -> attraction.copy(id = "attr.${city.id}", cityId = city.id) }

        return TripContent(
            packaged.trip.copy(cities = cities, attractions = attractions, walks = emptyList()),
            AssetResolver(packaged.trip.assets, exists = { false }),
        )
    }

    /** A day that lists [cityIds], sleeps in [base], and points its timeline at [pointsAt]. */
    private fun day(
        cityIds: List<String>,
        base: String?,
        pointsAt: List<String> = emptyList(),
    ): TripDay = TripDay(
        id = "day",
        date = "2026-09-29",
        dayNumber = 17,
        timeZone = "Europe/Zagreb",
        cityIds = cityIds,
        baseCityId = base,
        timeline = listOf(
            // A transport row comes first on almost every real day, and it
            // must not decide anything: it points at a leg, not at a place to
            // read about.
            TimelineItem(id = "t", startTime = "07:00", kind = "transport", title = "Saída", refId = "transport.x"),
        ) + pointsAt.mapIndexed { i, id ->
            TimelineItem(id = "a$i", startTime = "0$i:00", kind = "attraction", title = id, refId = id)
        },
    )

    private fun openAt(content: TripContent, day: TripDay) = exploreCitiesOf(content, day).openAtCityId
    private fun chips(content: TripContent, day: TripDay) = exploreCitiesOf(content, day).chips.map { it.cityId }

    // ------------------------------------------------------------ the rule

    @Test
    fun `the timeline decides, and it outranks the base`() {
        // Day 17: sleeps in Cilipi, which packages nothing; the day is
        // Dubrovnik's, and the timeline is the only field that says so.
        val content = content("dubrovnik", "cilipi", empty = setOf("cilipi"))
        val day = day(cityIds = listOf("dubrovnik", "cilipi"), base = "cilipi", pointsAt = listOf("attr.dubrovnik"))

        assertEquals("dubrovnik", openAt(content, day))
    }

    @Test
    fun `the first row that points at a place wins, not the first row`() {
        // Day 15: Mostar is the base, and the day is Blagaj, Kravice and
        // Pocitelj in that order. The 11:00 row is the first parada.
        val content = content("mostar", "blagaj", "kravice", "pocitelj")
        val day = day(
            cityIds = listOf("mostar", "blagaj", "kravice", "pocitelj"),
            base = "mostar",
            pointsAt = listOf("attr.blagaj", "attr.kravice", "attr.pocitelj"),
        )

        assertEquals("blagaj", openAt(content, day))
        assertEquals(listOf("mostar", "blagaj", "kravice", "pocitelj"), chips(content, day))
    }

    @Test
    fun `a timeline that names no place falls back to the base`() {
        // Day 3 flies Amsterdam to Ksamil: the rows are all transport, and
        // the base is where the day lands.
        val content = content("amsterdam", "ksamil")
        val day = day(cityIds = listOf("amsterdam", "ksamil"), base = "ksamil")

        assertEquals("ksamil", openAt(content, day))
        assertEquals(listOf("amsterdam", "ksamil"), chips(content, day))
    }

    @Test
    fun `a timeline pointing at a city with nothing to read is ignored`() {
        val content = content("dubrovnik", "cilipi", empty = setOf("cilipi"))
        val day = day(
            cityIds = listOf("dubrovnik", "cilipi"),
            base = "dubrovnik",
            // A row can name the base even when the base packages nothing;
            // that is not a reason to open an empty screen.
            pointsAt = listOf("attr.cilipi"),
        )

        assertEquals("dubrovnik", openAt(content, day))
    }

    @Test
    fun `a base with nothing and no timeline falls to the first city with content`() {
        val content = content("cilipi", "dubrovnik", empty = setOf("cilipi"))
        val day = day(cityIds = listOf("cilipi", "dubrovnik"), base = "cilipi")

        assertEquals("dubrovnik", openAt(content, day))
    }

    @Test
    fun `with nothing anywhere the base is still the floor`() {
        // Day 1: Sao Paulo, an airport and a night flight. No city of that day
        // packages anything, and the screen still has to name somewhere.
        val content = content("sao-paulo", empty = setOf("sao-paulo"))
        val day = day(cityIds = listOf("sao-paulo"), base = "sao-paulo")

        assertEquals("sao-paulo", openAt(content, day))
        assertEquals(emptyList<String>(), chips(content, day))
    }

    // ----------------------------------------------------------- the band

    @Test
    fun `one city with content draws no band at all`() {
        // Day 9 in Zabljak, and seven other single-city days. A row holding
        // one chip is a control that cannot be used.
        val content = content("zabljak")
        val day = day(cityIds = listOf("zabljak"), base = "zabljak")

        assertEquals(emptyList<String>(), chips(content, day))
        assertEquals("zabljak", openAt(content, day))
    }

    @Test
    fun `a city the day passes through with nothing packaged gets no chip`() {
        // Corfu, Sarande and Zagreb are on the route and on no screen.
        val content = content("ksamil", "corfu", "butrinto", empty = setOf("corfu"))
        val day = day(cityIds = listOf("ksamil", "corfu", "butrinto"), base = "ksamil")

        assertEquals(listOf("ksamil", "butrinto"), chips(content, day))
    }

    @Test
    fun `chips keep the order the day declares, not the order of arrival`() {
        val content = content("mostar", "blagaj", "kravice", "pocitelj")
        val day = day(
            cityIds = listOf("mostar", "blagaj", "kravice", "pocitelj"),
            base = "mostar",
            pointsAt = listOf("attr.pocitelj", "attr.blagaj"),
        )

        assertEquals(listOf("mostar", "blagaj", "kravice", "pocitelj"), chips(content, day))
        // …and the opening city is still the timeline's first, not the list's.
        assertEquals("pocitelj", openAt(content, day))
    }

    @Test
    fun `a walk row counts as a place, the same as an attraction row`() {
        val packaged = packagedContent()
        val base = content("sarajevo", "butmir")
        val walk = packaged.trip.walks.first().copy(id = "walk.butmir", cityId = "butmir")
        val withWalk = TripContent(
            base.trip.copy(
                walks = listOf(walk),
                cities = base.trip.cities.map {
                    if (it.id == "butmir") it.copy(walkIds = listOf(walk.id)) else it
                },
            ),
            base.assets,
        )
        val day = TripDay(
            id = "day",
            date = "2026-09-25",
            dayNumber = 13,
            timeZone = "Europe/Sarajevo",
            cityIds = listOf("sarajevo", "butmir"),
            baseCityId = "sarajevo",
            timeline = listOf(
                TimelineItem(id = "w", startTime = "09:00", kind = "walk", title = "Passeio", refId = walk.id),
            ),
        )

        assertEquals("butmir", exploreCitiesOf(withWalk, day).openAtCityId)
    }

    @Test
    fun `no day at all offers nothing and names nowhere`() {
        assertEquals(ExploreCities.None, exploreCitiesOf(content("sarajevo"), null))
    }

    @Test
    fun `an unknown city id in cityIds is not a chip and is not a crash`() {
        val content = content("sarajevo")
        val day = day(cityIds = listOf("sarajevo", "does.not.exist"), base = "sarajevo")

        assertEquals(emptyList<String>(), chips(content, day))
        assertEquals("sarajevo", openAt(content, day))
    }

    // --------------------------------------------- cityOfDay is untouched

    @Test
    fun `Explorar and the base disagree on purpose, and cityOfDay keeps the base`() {
        val content = content("dubrovnik", "cilipi", empty = setOf("cilipi"))
        val day = day(cityIds = listOf("dubrovnik", "cilipi"), base = "cilipi", pointsAt = listOf("attr.dubrovnik"))

        assertEquals("dubrovnik", openAt(content, day))
        // Screens 02, 03, 17 and 20 ask where the traveller sleeps, and that
        // answer does not move because Explorar answers something else.
        assertEquals(
            "cilipi",
            com.travelcompanion.app.domain.food.cityOfDay(content, day)?.id,
        )
    }

    private val City.attractionCount: Int get() = attractionIds.size

    @Test
    fun `the tie is never broken by counting attractions`() {
        // Cilipi is given more attractions than Dubrovnik, and the timeline
        // still decides. A count that usually agrees with the right answer is
        // a coincidence, not a guard (D089).
        val base = content("dubrovnik", "cilipi")
        val fat = TripContent(
            base.trip.copy(
                cities = base.trip.cities.map {
                    if (it.id == "cilipi") it.copy(attractionIds = it.attractionIds + "attr.cilipi.2") else it
                },
                attractions = base.trip.attractions +
                    base.trip.attractions.first().copy(id = "attr.cilipi.2", cityId = "cilipi"),
            ),
            base.assets,
        )
        val day = day(cityIds = listOf("cilipi", "dubrovnik"), base = "cilipi", pointsAt = listOf("attr.dubrovnik"))

        assertEquals(2, fat.city("cilipi")!!.attractionCount)
        assertEquals(1, fat.city("dubrovnik")!!.attractionCount)
        assertEquals("dubrovnik", openAt(fat, day))
    }
}
