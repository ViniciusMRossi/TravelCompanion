package com.travelcompanion.app.feature.city

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TimelineItem
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import com.travelcompanion.app.domain.explore.exploreCitiesOf
import com.travelcompanion.app.domain.food.cityOfDay
import com.travelcompanion.app.domain.today.TodayUseCase
import com.travelcompanion.app.feature.emergency.buildEmergencyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * The band above screen 04, and the cursor behind it.
 *
 * Two things are asserted here that a state test cannot reach: that the row is
 * absent on a single-city day, and that moving the cursor moves **only** this
 * screen. The second is the D089 shape — Explorar's city and the base agree on
 * sixteen of the twenty days, so a cursor shared with screens 02, 17 and 20
 * would look correct almost always and name the wrong country in Čilipi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class ExploreCityBandTest {

    @get:Rule
    val compose = createComposeRule()

    private val date: LocalDate = LocalDate.parse("2026-09-21")

    /**
     * A day that sleeps in Mostar and is spent in Sarajevo — the shape of days
     * 15 and 17, built from the sample package so it runs on a fresh clone.
     */
    private fun twoCityDay(): TripContent {
        val packaged = packagedContent()
        val sarajevo = packaged.trip.cities.single()
        val attraction = packaged.trip.attractions.first { it.cityId == sarajevo.id }
        val mostar = sarajevo.copy(
            id = "mostar",
            name = "Mostar",
            countryCode = sarajevo.countryCode,
            attractionIds = listOf("attr.mostar"),
            walkIds = emptyList(),
            storyIds = emptyList(),
            restaurants = emptyList(),
            audioGuideId = null,
        )
        val mostarAttraction = attraction.copy(id = "attr.mostar", cityId = "mostar")

        return TripContent(
            packaged.trip.copy(
                cities = listOf(sarajevo, mostar),
                attractions = packaged.trip.attractions + mostarAttraction,
                days = listOf(
                    packaged.trip.days.single().copy(
                        cityIds = listOf("mostar", sarajevo.id),
                        baseCityId = "mostar",
                        timeline = listOf(
                            TimelineItem(
                                id = "row.attraction",
                                startTime = "09:00",
                                kind = "attraction",
                                title = attraction.name,
                                refId = attraction.id,
                            ),
                        ),
                    ),
                ),
            ),
            AssetResolver(packaged.trip.assets, exists = { false }),
        )
    }

    /** The single-city day the package actually ships: day 9 in Sarajevo. */
    private fun oneCityDay(): TripContent = packagedContent()

    private fun day(content: TripContent): TripDay = content.days.single()

    /**
     * Screen 04 driven by the real cursor, not by a copy of it.
     *
     * `rememberExploreCityCursor` is the production composable; the test taps
     * chips and reads the same state the route reads.
     */
    private fun screen(content: TripContent): () -> String? {
        val cities = exploreCitiesOf(content, day(content))
        var read: () -> String? = { null }
        compose.setContent {
            val cursor = rememberExploreCityCursor(cities)
            read = { cursor.value }
            val state = buildCityState(content, cursor.value, date)!!
            CityScreen(
                state = state,
                onBack = null,
                onPlayGuide = {},
                onSeekToChapter = {},
                onOpenAttraction = {},
                onStartWalk = {},
                onPlayStory = {},
                onOpenAction = {},
                cities = cities.chips,
                onSelectCity = { cursor.value = it },
            )
        }
        return read
    }

    // ------------------------------------------------------- the band draws

    @Test
    fun `a day with two cities draws the band and opens on the timeline's city`() {
        val content = twoCityDay()

        val cursor = screen(content)

        compose.onNodeWithTag("city-band").assertIsDisplayed()
        compose.onNodeWithText("Mostar").assertIsDisplayed()
        assertEquals("sarajevo", cursor())
    }

    @Test
    fun `a day with one city draws no band at all`() {
        val content = oneCityDay()

        val cursor = screen(content)

        // The eight single-city days keep the screen they always had: hero,
        // intro, guide, cards — and nothing above them.
        compose.onAllNodesWithTag("city-band").assertCountEquals(0)
        assertEquals("sarajevo", cursor())
        // …and everything the screen used to draw is still drawn.
        compose.onNodeWithText("O QUE VER").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("HISTÓRIAS CURTAS").performScrollTo().assertIsDisplayed()
    }

    // --------------------------------------------------- the cursor is local

    @Test
    fun `moving the cursor moves Explorar and nothing else`() {
        val content = twoCityDay()
        val day = day(content)
        val base = day.baseCityId

        val cursor = screen(content)
        assertEquals("sarajevo", cursor())

        // Screens 02, 17 and 20 before the tap.
        val todayBefore = TodayUseCase(content)("vinicius", date, LocalTime.of(9, 0))!!.cityName
        val emergencyBefore = buildEmergencyState(content, date)!!.location
        val menuBefore = cityOfDay(content, day)?.id

        compose.onNodeWithText("Mostar").performClick()
        assertEquals("mostar", cursor())

        compose.onNodeWithText("Sarajevo").performClick()
        assertEquals("sarajevo", cursor())

        // The base is still the base, and all three screens still read it.
        assertEquals("mostar", base)
        assertEquals(base, menuBefore)
        assertEquals(menuBefore, cityOfDay(content, day)?.id)
        assertEquals(todayBefore, TodayUseCase(content)("vinicius", date, LocalTime.of(9, 0))!!.cityName)
        assertEquals(emergencyBefore, buildEmergencyState(content, date)!!.location)
        assertEquals("Mostar", todayBefore)
        assertEquals("Mostar", emergencyBefore.substringBefore(" ·").trim())
    }

    @Test
    fun `the cursor starts on the day's city every time the screen is composed`() {
        val content = twoCityDay()
        val cities = exploreCitiesOf(content, day(content))

        var cursorValue: String? = null
        compose.setContent {
            val cursor = rememberExploreCityCursor(cities)
            cursorValue = cursor.value
        }

        // Not `rememberSaveable`: re-entering the tab must not restore a city
        // chosen before the traveller walked somewhere else.
        assertEquals(cities.openAtCityId, cursorValue)
        assertNull(exploreCitiesOf(content, null).openAtCityId)
    }
}
