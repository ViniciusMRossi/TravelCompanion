package com.travelcompanion.app.feature.emergency

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.operations.WITHHELD_NOTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Screen 17 from the package.
 *
 * The split this file exists to hold: a public emergency number is a fact
 * about a country and stays dialable in a package of examples, while every
 * number that belongs to this trip does not (D064).
 */
class EmergencyStateTest {

    private val content = packagedContent(exists = { false })
    private val date = LocalDate.parse("2026-09-21")

    @Test
    fun `the public services stay dialable in a mock package`() {
        val state = buildEmergencyState(content, date)!!

        assertEquals("112", state.general.number)
        assertTrue(state.general.dialable)
        assertEquals("122", state.police!!.number)
        assertEquals("124", state.ambulance!!.number)
        assertNull(state.general.note)
    }

    /**
     * `+000000000` is a placeholder and pressing it would fail at the moment
     * this screen exists for. The row stays and says why.
     */
    @Test
    fun `the trip's own numbers are withheld while the content is mock`() {
        val state = buildEmergencyState(content, date)!!

        assertEquals(3, state.contacts.size)
        state.contacts.forEach { contact ->
            assertFalse(contact.label, contact.dialable)
            assertNull(contact.number)
            assertEquals(WITHHELD_NOTE, contact.note)
        }
        assertEquals(
            listOf("Seguro viagem", "Hospedagem em Sarajevo — exemplo", "Representação brasileira"),
            state.contacts.map { it.label },
        )
    }

    /** A screen reader on this screen says who is being called. */
    @Test
    fun `every contact carries its own accessibility label`() {
        val state = buildEmergencyState(content, date)!!

        assertEquals("Ligar para Emergência geral", state.general.accessibilityLabel)
        assertEquals("Ligar para Polícia", state.police!!.accessibilityLabel)
        assertEquals(
            "Ligar para a hospedagem, Hospedagem em Sarajevo — exemplo",
            state.contacts[1].accessibilityLabel,
        )
        assertEquals("Onde estão as malas", state.contacts[1].detail)
    }

    @Test
    fun `the location is where the traveller is, in plain words`() {
        val state = buildEmergencyState(content, date)!!
        assertEquals("Sarajevo · Bósnia e Herzegovina", state.location)
    }

    @Test
    fun `the card shown to a stranger speaks the local language first`() {
        val card = buildEmergencyState(content, date)!!.showToSomeone
        assertNotNull(card)
        assertEquals("Trebam pomoć. Molim vas, pozovite hitnu službu.", card!!.localLanguageText)
        assertEquals(
            "Preciso de ajuda. Por favor, chame o serviço de emergência.",
            card.translation,
        )
    }
}
