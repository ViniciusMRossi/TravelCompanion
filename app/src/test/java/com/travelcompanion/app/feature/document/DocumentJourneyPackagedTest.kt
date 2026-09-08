package com.travelcompanion.app.feature.document

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Both legs of the two tickets that have two, from the package that ships.
 *
 * `firstOrNull` read the first transport naming the document, which is D097's
 * shape — the first element taken without a criterion. Two packaged documents
 * are named by two transports each, and the element it happened to return was
 * the wrong one in both: on 02/10, the day of the flight home, the LATAM
 * ticket drew São Paulo → Amsterdã; and the Croatia ticket opened at the
 * Zagreb gate showing the Dubrovnik leg already flown, 1h15 earlier the same
 * morning (D155).
 *
 * `assets/trip-production/` is not in the repository (D027, D028), so this
 * asserts nothing when the package is absent; `DocumentStateTest` builds the
 * same two-leg shape by hand.
 */
class DocumentJourneyPackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { true }))
    }

    private fun legsOf(content: TripContent, documentId: String) =
        buildDocumentState(content, documentId)!!.journey
            .map { Triple(it.originTime, it.originName, it.destinationName) }

    /** Two legs, 1h15 apart, on one morning. The connection is the ticket. */
    @Test
    fun `the Croatia ticket draws Dubrovnik to Zagreb and Zagreb to Amsterda`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)

        assertEquals(
            listOf(
                Triple("06:15", "Dubrovnik · aeroporto", "Zagreb · aeroporto"),
                Triple("08:25", "Zagreb · aeroporto", "Amsterdã · Schiphol"),
            ),
            legsOf(content!!, "doc.ticket.dubrovnik-zagreb-amsterda"),
        )
    }

    /** "Voos São Paulo ⇄ Amsterdã": the title already promised both. */
    @Test
    fun `the LATAM ticket carries the way out and the way home`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)

        assertEquals(
            listOf(
                Triple("18:00", "São Paulo · Guarulhos", "Amsterdã · Schiphol"),
                Triple("13:10", "Amsterdã · Schiphol", "São Paulo · Guarulhos"),
            ),
            legsOf(content!!, "doc.ticket.latam-saopaulo-amsterdam"),
        )
    }

    /**
     * And nothing else moved.
     *
     * Five documents in the package are named by exactly one transport and
     * still draw exactly one block; the other twenty are named by none and
     * draw none, as before.
     */
    @Test
    fun `only the two connections have more than one leg`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val byLegCount = content.trip.documents
            .groupingBy { buildDocumentState(content, it.id)!!.journey.size }
            .eachCount()

        assertEquals(mapOf(0 to 20, 1 to 5, 2 to 2), byLegCount)
    }

    /** The two fields that describe the whole ticket were not touched. */
    @Test
    fun `a connection keeps one locator and declares no price`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        listOf(
            "doc.ticket.dubrovnik-zagreb-amsterda" to "96LE2K",
            "doc.ticket.latam-saopaulo-amsterdam" to "LIIORY",
        ).forEach { (documentId, expected) ->
            val state = buildDocumentState(content, documentId)!!
            assertEquals(documentId, expected, state.locator)
            assertEquals(documentId, null, state.price)
        }
    }
}
