package com.travelcompanion.app.feature.transport

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.operations.ActionWindow
import com.travelcompanion.app.domain.operations.WITHHELD_NOTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Screen 15 from the package, including what it refuses to offer. */
class TransportStateTest {

    private val content = packagedContent(exists = { false })
    private val bus = "transport.sarajevo-mostar.bus"

    @Test
    fun `the leg is drawn from both ends, with the critical item apart`() {
        val state = buildTransportState(content, bus, LocalTime.of(12, 0))!!

        assertEquals("19:30", state.origin.time)
        assertEquals("Sarajevo · Autobuska stanica", state.origin.stationName)
        assertEquals("A confirmar", state.origin.platform)
        assertEquals("22:00", state.destination.time)
        assertEquals("2h30", state.durationLabel)
        assertNotNull("the critical item is its own block", state.critical)
        assertEquals("Esteja na estação até 19:00.", state.critical?.instruction)
        assertEquals("19:30", state.critical?.nominalTime)
    }

    @Test
    fun `the window moves with the clock`() {
        assertEquals(ActionWindow.Ahead, buildTransportState(content, bus, LocalTime.of(12, 0))!!.window)
        assertEquals(ActionWindow.Now, buildTransportState(content, bus, LocalTime.of(19, 10))!!.window)
        assertEquals(ActionWindow.Passed, buildTransportState(content, bus, LocalTime.of(20, 0))!!.window)
    }

    @Test
    fun `the ticket and the plan B the package names are both reachable`() {
        val state = buildTransportState(content, bus, LocalTime.of(12, 0))!!
        assertEquals("ticket.sarajevo-mostar", state.documentId)
        assertEquals("planb.day09", state.planB?.id)
        assertEquals("Vinícius · Érika", state.peopleLabel?.substringAfter("· "))
    }

    /**
     * The package declares no phone action on this transport, so there is no
     * operator row — and if it did, the mock gate would withhold it (D064).
     */
    @Test
    fun `an operator phone from a mock package is not dialable`() {
        val state = buildTransportState(content, bus, LocalTime.of(12, 0))!!
        state.operatorPhone?.let { phone ->
            assertFalse(phone.dialable)
            assertEquals(WITHHELD_NOTE, phone.note)
        }
        assertTrue("nothing to dial in this package", state.operatorPhone?.dialable != true)
    }

    /**
     * The eyebrow the approved artboard puts above the journey — "Ônibus ·
     * Centrotrans" — which the app had never drawn, plus the service number
     * the package declares and nothing read (D177).
     *
     * The companion to `TransportServicePackagedTest`, on the packaged sample
     * so it runs on every machine. The sample bus declares no number, so this
     * half also states what a leg without one looks like.
     */
    @Test
    fun `the leg is named by mode and operator, and by its number when it has one`() {
        val state = buildTransportState(content, bus, LocalTime.of(12, 0))!!

        assertEquals("Ônibus · Operadora — exemplo", state.serviceLine)

        val trip = content.trip
        val numbered = com.travelcompanion.app.data.trip.TripContent(
            trip.copy(
                transports = trip.transports.map { transport ->
                    if (transport.id == bus) {
                        transport.copy(type = "flight", operator = "Croatia Airlines", serviceNumber = "OU 661")
                    } else {
                        transport
                    }
                },
            ),
            content.assets,
        )

        assertEquals(
            "Voo · Croatia Airlines · OU 661",
            buildTransportState(numbered, bus, LocalTime.of(12, 0))!!.serviceLine,
        )
    }

    @Test
    fun `an unknown transport is nothing to build`() {
        assertNull(buildTransportState(content, "does.not.exist", LocalTime.NOON))
        assertNull(buildTransportState(content, null, LocalTime.NOON))
    }
}
