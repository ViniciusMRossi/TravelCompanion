package com.travelcompanion.app.feature.emergency

import com.travelcompanion.app.data.trip.EmergencyContact
import com.travelcompanion.app.data.trip.EmergencyProfile
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
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

    /**
     * A second country, so the tests can tell "the profile for this country"
     * apart from "the first profile in the list". Croatia's own numbers, which
     * differ from Bosnia's — with one profile in the fixture the two readings
     * are indistinguishable, which is what let the defect through.
     */
    private val croatia = EmergencyProfile(
        id = "emergency.hr",
        countryCode = "HR",
        countryName = "Croácia",
        generalEmergency = EmergencyContact(label = "Emergência geral", phone = "112"),
        police = EmergencyContact(label = "Polícia", phone = "192"),
        ambulance = EmergencyContact(label = "Ambulância", phone = "194"),
    )

    /** The packaged trip with [croatia] first in the list, and the day's city
     *  moved to [countryCode] — everything else untouched. */
    private fun crossingTheBorder(
        countryCode: String,
        profiles: List<EmergencyProfile> = listOf(croatia) + content.trip.emergencyProfiles,
    ): TripContent {
        val cities = content.trip.cities.map { city ->
            if (city.id == "sarajevo") {
                city.copy(
                    countryCode = countryCode,
                    countryName = if (countryCode == "HR") "Croácia" else city.countryName,
                )
            } else {
                city
            }
        }
        return TripContent(
            content.trip.copy(cities = cities, emergencyProfiles = profiles),
            content.assets,
        )
    }

    /**
     * The defect this file exists to hold: the screen took the first profile in
     * the package, so a trip crossing a border would have offered one country's
     * police from another country's city (D070).
     */
    @Test
    fun `the services are the ones of the country the traveller is in`() {
        val inBosnia = buildEmergencyState(crossingTheBorder("BA"), date)!!
        assertEquals("122", inBosnia.police!!.number)
        assertEquals("124", inBosnia.ambulance!!.number)
        assertEquals("Sarajevo · Bósnia e Herzegovina", inBosnia.location)

        val inCroatia = buildEmergencyState(crossingTheBorder("HR"), date)!!
        assertEquals("192", inCroatia.police!!.number)
        assertEquals("194", inCroatia.ambulance!!.number)
        assertEquals("Sarajevo · Croácia", inCroatia.location)
    }

    /**
     * A package missing the profile for a country it enters falls back to the
     * first one — better than a screen with no emergency number at all — and
     * the location line still says where the traveller actually is (D070).
     */
    @Test
    fun `with no profile for this country the first one is the fallback`() {
        val state = buildEmergencyState(
            crossingTheBorder("BA", profiles = listOf(croatia)),
            date,
        )!!

        assertEquals("192", state.police!!.number)
        assertEquals("Sarajevo · Bósnia e Herzegovina", state.location)
    }

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
    fun `a trip with no emergency profile at all is nothing to build`() {
        val without = TripContent(
            content.trip.copy(emergencyProfiles = emptyList()),
            content.assets,
        )
        assertNull(buildEmergencyState(without, date))
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
