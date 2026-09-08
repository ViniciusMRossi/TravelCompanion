package com.travelcompanion.app.feature.city

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Screen 04 — the editorial layer of a city. */
class CityStateTest {

    private val content = packagedContent(exists = { false })
    private val today = LocalDate.parse("2026-09-21")

    private fun state(content: TripContent = this.content) =
        buildCityState(content, "sarajevo", today)!!

    @Test
    fun `the city guide names its chapters and their length`() {
        val guide = state().guide!!

        assertEquals("3 capítulos · 34 min", guide.summary)
        assertEquals(3, guide.chapters.size)
        assertEquals(listOf(0, 1, 2), guide.chapters.map { it.index })
        // Each chapter runs until the next one starts, and the last until the
        // guide ends.
        assertTrue(guide.chapters.all { it.durationLabel.endsWith(" min") })
    }

    /**
     * The audio for the city guide is not in this build, and the card says so
     * rather than promising offline (D013, D021).
     */
    @Test
    fun `a guide whose file is missing is not called saved`() {
        assertFalse(state().guide!!.isPackaged)

        val everything = TripContent(
            content.trip,
            AssetResolver(content.trip.assets, exists = { true }),
        )
        assertTrue(state(everything).guide!!.isPackaged)
    }

    @Test
    fun `an attraction says where it sits in the itinerary`() {
        val attractions = state().attractions

        assertEquals(listOf("Baščaršija", "Latin Bridge"), attractions.map { it.name })
        assertEquals("09:40", attractions.first().scheduleTime)
        // Latin Bridge is in the city but not on the day's timeline.
        assertNull(attractions.last().scheduleTime)
    }

    /** The badge is earned by a file in this build, never by a promise. */
    @Test
    fun `Offline is a pill only when the audio is really here`() {
        // The price is no longer among them: a card in a carousel has less
        // room than screen 05, and the packaged prices are sentences (D154).
        assertEquals(listOf("Audioguia"), state().attractions.first().pills)

        val everything = TripContent(
            content.trip,
            AssetResolver(content.trip.assets, exists = { true }),
        )
        assertEquals(
            listOf("Audioguia", "Offline"),
            state(everything).attractions.first().pills,
        )
    }

    @Test
    fun `the walk card carries distance, duration and how many stories`() {
        val walk = state().walk!!

        assertEquals("1,8 km", walk.distanceLabel)
        assertEquals("45 min", walk.durationLabel)
        assertEquals("2 histórias", walk.storiesLabel)
    }

    @Test
    fun `where to eat keeps the practical note and the walking distance`() {
        val restaurant = state().restaurants.single()

        assertEquals("8 min a pé", restaurant.distanceLabel)
        assertEquals("Exemplo de nota prática.", restaurant.practicalNote)
        assertEquals("maps", restaurant.action!!.kind)
    }

    @Test
    fun `a story carries both what it opens with and what it says`() {
        val stories = state().stories

        assertEquals(2, stories.size)
        assertEquals("2 min", stories.first().durationLabel)
        assertTrue(stories.first().body.isNotBlank())
        // Only one of the two has audio, so only that one offers "Ouvir".
        assertNull(stories.first().audioGuideId)
        assertEquals("ag.latin-bridge", stories.last().audioGuideId)
    }

    @Test
    fun `the hero names the stretch of the trip spent here`() {
        assertEquals("21 de setembro", state().stayLabel)
    }

    /** Contexto, não operação: no platform, no deadline, no locator. */
    @Test
    fun `nothing operational reaches the editorial opening`() {
        val intro = state().intro
        listOf("19:30", "Plataforma", "MOCK-", "check-in").forEach { operational ->
            assertFalse(operational, intro.contains(operational, ignoreCase = true))
        }
    }

    @Test
    fun `a city that is not in the package is nothing to build`() {
        assertNull(buildCityState(content, "does.not.exist", today))
    }
}
