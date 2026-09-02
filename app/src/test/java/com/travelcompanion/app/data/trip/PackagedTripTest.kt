package com.travelcompanion.app.data.trip

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The packaged trip is the app's content contract. If it stops deserialising,
 * every screen goes blank offline, so this parses the real file rather than a
 * fixture that could drift away from it.
 */
class PackagedTripTest {

    @Test
    fun packagedTripParsesIntoTheTypedModel() {
        val content = packagedContent()

        assertEquals("balkans-2026", content.info.id)
        assertEquals(listOf("vinicius", "erika"), content.info.participants.map { it.id })
        assertTrue("packaged trip must contain at least one day", content.days.isNotEmpty())
    }

    @Test
    fun everyTimelineReferencePointsAtSomethingThatExists() {
        val content = packagedContent()

        content.days.forEach { day ->
            day.timeline.forEach { item ->
                val refId = item.refId ?: return@forEach
                val target: Any? = when (item.kind) {
                    "attraction" -> content.attraction(refId)
                    "walk" -> content.walk(refId)
                    "transport" -> content.transport(refId)
                    "accommodation" -> content.accommodation(refId)
                    else -> Unit
                }
                assertNotNull("unresolved ${item.kind} reference: $refId", target)
            }
        }
    }

    @Test
    fun criticalItemsKeepNominalTimeAndDeadlineApart() {
        val content = packagedContent()
        val day = content.days.first()

        val bus = content.criticalItemsFor(day).single { it.id == "critical.bus.sarajevo-mostar" }

        assertEquals("19:30", bus.nominalTime)
        assertEquals("19:00", bus.actionByTime)
        assertTrue(bus.instruction.isNotBlank())
    }

    @Test
    fun criticalItemsAreCollectedFromTheDayAndFromItsEntities() {
        val content = packagedContent()
        val day = content.days.first()

        // The bus deadline is declared on the day; the reception closing time
        // only exists on the accommodation. Today must surface both.
        assertEquals(
            listOf("critical.bus.sarajevo-mostar", "critical.stay.sarajevo.reception"),
            content.criticalItemsFor(day).map { it.id }.sorted(),
        )
    }

    @Test
    fun criticalItemsAreNotDuplicatedByTheirOwningEntity() {
        val content = packagedContent()
        val day = content.days.first()

        // The same critical item is declared on the day and on the transport.
        val ids = content.criticalItemsFor(day).map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun missingAssetBinariesResolveToNullRatherThanThrowing() {
        val content = packagedContent()

        // No photography or audio is packaged yet; heroes must fall back.
        assertNull(content.assets.packagedPathIfPresent("img.sarajevo.hero"))
        assertNull(content.assets.uri("audio.latin-bridge"))
        assertEquals(
            "trip/images/cities/sarajevo.jpg",
            content.assets.packagedPath("img.sarajevo.hero"),
        )
    }

    companion object {
        /** The packaged file, read from the module's own assets. */
        fun packagedContent(exists: (String) -> Boolean = { false }): TripContent {
            val file = File("src/main/assets/trip/trip.json")
            assertTrue("packaged trip.json not found at ${file.absolutePath}", file.exists())
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            val trip = json.decodeFromString<TripPackage>(file.readText())
            return TripContent(trip, AssetResolver(trip.assets, exists))
        }
    }
}
