package com.travelcompanion.app.feature.transport

import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripPackage
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalTime

/**
 * How screen 15 names the leg, measured against the real package.
 *
 * The approved artboard names it in an eyebrow above the vertical journey —
 * "Ônibus · Centrotrans", mode then operator — and the app drew no such row at
 * all. `serviceNumber` was parsed and read nowhere, so five flights declared
 * `LA 8078`, `EJU7913`, `OU 661`, `OU 450` and `LA 8079` and the screen never
 * said one of them. At 04:45 on 30/09 the number is what the airport board
 * lists and what the counter asks for (D177).
 *
 * `assets/trip-production/` is not in the repository (D027, D028), so this
 * asserts nothing when the package is absent. `TransportStateTest` holds the
 * mode-and-operator half on the packaged sample and runs on every machine.
 */
class TransportServicePackagedTest {

    private val file = File("src/main/assets/trip-production/trip.json")

    private fun packaged(): TripContent? {
        if (!file.exists()) return null
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val trip = json.decodeFromString<TripPackage>(file.readText())
        return TripContent(trip, AssetResolver(trip.assets, exists = { true }))
    }

    @Test
    fun `the flight to Zagreb says OU 661`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val state = buildTransportState(
            content,
            "transport.dubrovnik-zagreb.ou661",
            LocalTime.of(4, 45),
        )!!

        assertEquals("Voo · Croatia Airlines · OU 661", state.serviceLine)
        assertTrue("the number itself, not an abbreviation", state.serviceLine.contains("OU 661"))
    }

    @Test
    fun `every service number the package declares is on its screen`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        var declared = 0
        content.trip.transports.forEach { transport ->
            val number = transport.serviceNumber ?: return@forEach
            declared++
            val state = buildTransportState(content, transport.id, LocalTime.NOON)!!
            assertTrue(
                "${transport.id}: $number must be on screen 15 — was '${state.serviceLine}'",
                state.serviceLine.contains(number),
            )
        }

        assertEquals("the package's service numbers", 5, declared)
    }

    /** And a leg that declares none is named exactly as the artboard names it. */
    @Test
    fun `a leg without a number is still named by mode and operator`() {
        val content = packaged()
        assumeTrue("operational package not packaged in this checkout", content != null)
        content!!

        val state = buildTransportState(content, "transport.kotor-zabljak.bus", LocalTime.NOON)!!

        assertEquals("Ônibus · RIO Travel d.o.o.", state.serviceLine)
    }
}
