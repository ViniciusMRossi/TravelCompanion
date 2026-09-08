package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * `practical.requirements` on screen 05, measured against the real 47.
 *
 * Fifteen attractions carry the list and between them they hold thirty
 * sentences, the longest 135 characters: "Estar na Westermarkt às 15h20: uma
 * vez iniciado o programa, ninguém mais entra, e a casa não remarca nem
 * reembolsa em nenhuma hipótese." Until D176 nothing on the screen read the
 * field, so that sentence — timed entry, no rebooking, no refund — was written
 * and dropped. The assertion is the same one D154 made for `price`: every
 * sentence arrives whole, character for character, because screen 05 draws
 * `requirements` in one place and a cut here has no second chance.
 *
 * `assets/trip-production/` is not in the repository (D027, D028), so this
 * asserts nothing when the package is absent. `AttractionStateTest` holds the
 * same guarantee on a hand-built fixture and runs on every machine.
 */
class AttractionRequirementsPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { true }))
    }

    @Test
    fun `every requirement in the package reaches the screen uncut`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val date = LocalDate.parse(content.days.first().date)
        var lists = 0
        var sentences = 0

        content.trip.attractions.forEach { attraction ->
            val required = attraction.practical?.requirements.orEmpty()
            if (required.isEmpty()) return@forEach
            lists++

            val state = buildAttractionState(content, attraction.id, date)!!
            required.forEach { sentence ->
                sentences++
                assertTrue(
                    "${attraction.id}: requirement must arrive whole, " +
                        "${sentence.length} chars — $sentence",
                    state.requirements.contains(sentence),
                )
            }
            assertEquals("${attraction.id}: in the package's own order", required, state.requirements)
        }

        assertEquals("the package's requirement lists", 15, lists)
        assertEquals("and the sentences in them", 30, sentences)
    }

    /** The longest one in the package, named, so the number cannot drift. */
    @Test
    fun `the Anne Frank house keeps the half that says there is no refund`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val date = LocalDate.parse(content.days.first().date)
        val state = buildAttractionState(content, "attr.amsterdam.anne-frank", date)!!
        val timed = state.requirements.first()

        assertEquals(135, timed.length)
        assertTrue(timed.startsWith("Estar na Westermarkt às 15h20"))
        assertTrue(timed.endsWith("não remarca nem reembolsa em nenhuma hipótese."))
    }

    /** The three one-line fields, on the same forty-seven. */
    @Test
    fun `best time, accessibility and a required reservation are all drawn`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val date = LocalDate.parse(content.days.first().date)
        var bestTime = 0
        var accessibility = 0
        var reservations = 0

        content.trip.attractions.forEach { attraction ->
            val practical = attraction.practical ?: return@forEach
            val lines = buildAttractionState(content, attraction.id, date)!!.practicalLines

            practical.bestTime?.let {
                bestTime++
                assertTrue(
                    "${attraction.id}: best time whole, ${it.length} chars",
                    lines.contains(PracticalLineUi("Melhor hora", it)),
                )
            }
            practical.accessibility?.let {
                accessibility++
                assertTrue(
                    "${attraction.id}: accessibility whole, ${it.length} chars",
                    lines.contains(PracticalLineUi("Acessibilidade", it)),
                )
            }
            if (practical.reservationRequired == true) {
                reservations++
                assertTrue(
                    "${attraction.id}: a required reservation is stated",
                    lines.any { it.label == "Reserva" },
                )
            }
            if (practical.reservationRequired == false) {
                assertTrue(
                    "${attraction.id}: a declared false draws nothing",
                    lines.none { it.label == "Reserva" },
                )
            }
        }

        assertEquals("best time in the package", 21, bestTime)
        assertEquals("accessibility in the package", 6, accessibility)
        assertEquals("reservations the package requires", 3, reservations)
    }
}
