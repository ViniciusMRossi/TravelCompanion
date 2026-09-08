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
 * The practical text of screen 05, measured against the real 47.
 *
 * `TcChip` draws one line and ellipsises (D055, and the clause is right), so a
 * chip is only ever a container for what the app itself composes. The package
 * that ships carries `practical.price` strings up to **141 characters** whose
 * second half is the instruction — "mas não cobre o teleférico", "Levem os
 * €10", "só em dinheiro: não aceitam cartão nem euro" — and screen 05 draws
 * `practical` in exactly one place, so what the ellipsis ate was gone for
 * good. Ten strings were over 45 characters when this test was written, and
 * it named all ten (D154).
 *
 * `assets/trip-production/` is not in the repository (D027, D028), so this
 * asserts nothing when the package is absent. `AttractionStateTest` holds the
 * same guarantee on the hand-built fixture and runs on every machine.
 */
class AttractionPracticalPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { true }))
    }

    private fun statesOf(content: TripContent): List<AttractionUiState> {
        val date = LocalDate.parse(content.days.first().date)
        return content.trip.attractions.map { buildAttractionState(content, it.id, date)!! }
    }

    /** A chip holds only what the app writes from a number, and stays short. */
    @Test
    fun `no chip on screen 05 is long enough to be cut`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val tooLong = content.trip.attractions.flatMap { attraction ->
            val date = LocalDate.parse(content.days.first().date)
            buildAttractionState(content, attraction.id, date)!!.chips
                .filter { it.length > 45 }
                .map { "${attraction.id} (${it.length}): $it" }
        }

        assertEquals(tooLong.joinToString("\n"), emptyList<String>(), tooLong)
    }

    /**
     * And the sentences the chips gave up are on the screen, whole.
     *
     * Every `price` and every `openingHours` in the package, character for
     * character — 22 strings over 47 attractions, 17 of them prices.
     */
    @Test
    fun `every practical string in the package reaches the screen uncut`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val date = LocalDate.parse(content.days.first().date)
        var counted = 0

        content.trip.attractions.forEach { attraction ->
            val practical = attraction.practical ?: return@forEach
            val lines = buildAttractionState(content, attraction.id, date)!!.practicalLines

            practical.price?.let { price ->
                counted++
                assertTrue(
                    "${attraction.id}: price must arrive whole, ${price.length} chars",
                    lines.contains(PracticalLineUi("Entrada", price)),
                )
            }
            practical.openingHours?.let { hours ->
                counted++
                assertTrue(
                    "${attraction.id}: opening hours must arrive whole, ${hours.length} chars",
                    lines.contains(PracticalLineUi("Horários", hours)),
                )
            }
        }

        assertEquals("the package's practical strings", 22, counted)
        assertEquals(
            "and 17 of them are prices",
            17,
            content.trip.attractions.count { it.practical?.price != null },
        )
    }

    /** The longest one in the package, named, so the number cannot drift. */
    @Test
    fun `the Dubrovnik Pass keeps the half that says what it does not cover`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val date = LocalDate.parse(content.days.first().date)
        val state = buildAttractionState(content, "attr.dubrovnik.muralhas", date)!!
        val entrada = state.practicalLines.single { it.label == "Entrada" }.value

        assertEquals(141, entrada.length)
        assertTrue(entrada.endsWith("mas não cobre o teleférico"))
    }

    /** No chip anywhere carries the source's sentences any more. */
    @Test
    fun `the chips of all forty-seven are only what the app composed`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val vocabulary = Regex("""^(Audioguia \d+ min|Salvo offline|Visita ~\d+ min)$""")
        val strange = statesOf(content).flatMap { it.chips }.filterNot(vocabulary::matches)

        assertEquals(emptyList<String>(), strange)
    }
}
