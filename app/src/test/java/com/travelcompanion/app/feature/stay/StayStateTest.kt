package com.travelcompanion.app.feature.stay

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.operations.ActionWindow
import com.travelcompanion.app.domain.operations.WITHHELD_NOTE
import com.travelcompanion.app.domain.today.BookingStatusUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

/** Screen 16 from the package, including the two Phase 1 substitutes. */
class StayStateTest {

    private val content = packagedContent(exists = { false })
    private val stay = "stay.sarajevo"

    @Test
    fun `check-in, check-out and status come from the package`() {
        val state = buildStayState(content, stay, LocalTime.of(12, 0))!!

        assertEquals("14:00 — 23:00", state.checkIn)
        assertEquals("até 10:00", state.checkOut)
        assertEquals(BookingStatusUi.Paid, state.status)
        assertEquals("voucher.sarajevo", state.voucherId)
        assertNotNull(state.instructions)
    }

    /**
     * No photography is packaged, so the hero has nothing to resolve and draws
     * the approved striped placeholder. A Phase 1 item, not a divergence.
     */
    @Test
    fun `the hero has no photograph to resolve`() {
        val state = buildStayState(content, stay, LocalTime.of(12, 0))!!
        assertNull(state.heroAssetPath)
        assertEquals("foto — Hospedagem em Sarajevo — exemplo", state.heroCaption)
    }

    @Test
    fun `the reception window opens at the act-by time`() {
        assertEquals(ActionWindow.Ahead, buildStayState(content, stay, LocalTime.of(22, 0))!!.window)
        assertEquals(ActionWindow.Now, buildStayState(content, stay, LocalTime.of(22, 45))!!.window)
    }

    /** The host's number is `+387000000000` in a package marked as examples. */
    @Test
    fun `the host cannot be dialled from a mock package`() {
        val phone = buildStayState(content, stay, LocalTime.of(12, 0))!!.hostPhone!!

        assertFalse(phone.dialable)
        assertNull(phone.number)
        assertEquals(WITHHELD_NOTE, phone.note)
        assertEquals("Ligar para o anfitrião", phone.label)
    }

    @Test
    fun `an unknown stay is nothing to build`() {
        assertNull(buildStayState(content, "does.not.exist", LocalTime.NOON))
    }
}
