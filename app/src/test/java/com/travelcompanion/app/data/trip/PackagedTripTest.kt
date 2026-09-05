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
    fun mergingDuplicateCriticalItemsLosesNoActionOrField() {
        val content = packagedContent()
        val day = content.days.first()
        val transport = content.transport("transport.sarajevo-mostar.bus")!!

        val declaredOnDay = day.criticalItems.single { it.id == "critical.bus.sarajevo-mostar" }
        val declaredOnTransport = transport.criticalItems.single { it.id == "critical.bus.sarajevo-mostar" }
        val merged = content.criticalItemsFor(day).single { it.id == "critical.bus.sarajevo-mostar" }

        // The day declares one action, the transport declares two. Nothing may
        // be dropped just because the same item was declared twice.
        val expected = (declaredOnDay.actionLinks + declaredOnTransport.actionLinks)
            .map { it.uri }
            .distinct()
        assertEquals(expected.sorted(), merged.actionLinks.map { it.uri }.sorted())
        assertEquals(
            listOf("Mostrar passagem", "Ver ponto de embarque"),
            merged.actionLinks.map { it.label }.sorted(),
        )
        assertNotNull("the reason only exists on the transport copy", merged.reason)
        assertEquals("19:30", merged.nominalTime)
        assertEquals("19:00", merged.actionByTime)
    }

    @Test
    fun mergingIsSymmetricAboutWhichCopyWasSeenFirst() {
        val content = packagedContent()
        val day = content.days.first()
        val transport = content.transport("transport.sarajevo-mostar.bus")!!
        val fromDay = day.criticalItems.single { it.id == "critical.bus.sarajevo-mostar" }
        val fromTransport = transport.criticalItems.single { it.id == "critical.bus.sarajevo-mostar" }

        val forward = fromDay.mergedWith(fromTransport)
        val backward = fromTransport.mergedWith(fromDay)

        assertEquals(
            forward.actionLinks.map { it.uri }.toSet(),
            backward.actionLinks.map { it.uri }.toSet(),
        )
        assertEquals(forward.reason, backward.reason)
    }

    @Test
    fun theTripWindowMatchesTheDayNumbersItPublishes() {
        val content = packagedContent()

        // "Dia 9 de 21" has to be arithmetic, not a coincidence.
        assertEquals(21, content.totalDays)
        content.days.forEach { day ->
            val expected = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(content.info.startDate),
                java.time.LocalDate.parse(day.date),
            ).toInt() + 1
            assertEquals("dayNumber must match the calendar", expected, day.dayNumber)
        }
    }

    @Test
    fun aWalkNamesTheAttractionItDepartsFrom() {
        val content = packagedContent()
        val walk = content.walk("walk.sarajevo.historical")!!

        assertEquals("bascarsija", walk.startAttractionId)
        assertNotNull("and that attraction must exist", content.attraction(walk.startAttractionId))
    }

    /**
     * Schema 1.1: every day and every transport endpoint states the zone its
     * wall-clock times are written in. The packaged trip stays inside
     * Europe/Sarajevo, so no timeline item may carry an override — an override
     * that agrees with its day is noise that will later disagree with it.
     */
    @Test
    fun everyPackagedLocalTimeNamesTheZoneItIsWrittenIn() {
        val content = packagedContent()
        val zones = java.time.ZoneId.getAvailableZoneIds()

        content.trip.cities.forEach { city ->
            assertTrue("city '${city.id}': unknown zone ${city.timeZone}", city.timeZone in zones)
        }
        content.days.forEach { day ->
            assertEquals("Europe/Sarajevo", day.timeZone)
            day.timeline.forEach { item ->
                assertNull(
                    "timeline '${item.id}' must inherit the day, not repeat it",
                    item.timeZone,
                )
            }
        }
        content.trip.transports.forEach { transport ->
            assertEquals("Europe/Sarajevo", transport.origin.timeZone)
            assertEquals("Europe/Sarajevo", transport.destination.timeZone)
        }
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
            return TripContent(trip, AssetResolver(trip.assets, exists = exists))
        }
    }
}
