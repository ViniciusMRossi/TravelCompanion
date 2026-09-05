package com.travelcompanion.app.feature.emergency

import com.travelcompanion.app.data.trip.EmergencyContact
import com.travelcompanion.app.data.trip.EmergencyProfile
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TwoCityTrip
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

    private companion object {
        val COUNTRY_NAMES = mapOf("HR" to "Croácia", "BR" to "Brasil")
    }

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

    /** The seventh profile of the real package: no single number, and 190. */
    private val brazil = EmergencyProfile(
        id = "emergency.br",
        countryCode = "BR",
        countryName = "Brasil",
        generalEmergency = EmergencyContact(label = "Emergência geral", phone = "190"),
        ambulance = EmergencyContact(label = "SAMU", phone = "192"),
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
                    countryName = COUNTRY_NAMES[countryCode] ?: city.countryName,
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
    /**
     * Found by opening screen 17 on day 3 of the real package (D090).
     *
     * A day that crosses a border lists every city it touches, and this screen
     * took the first — the one left that morning. On 15 September that is
     * Amsterdam: the screen said "Amsterdã · Países Baixos" and offered the
     * Dutch consulate to a traveller sleeping in Ksamil, directly above a row
     * reading "Guesthouse em Ksamil · Onde estão as malas". Albania's own
     * ambulance and the Tirana duty phone were not on the screen at all.
     *
     * Where the traveller is, is where the day says they sleep, which is what
     * screens 02 and 03 already read. Unreachable with a package whose days
     * touch one city each.
     */
    @Test
    fun `on a day that crosses a border the country is where the night is spent`() {
        val crossing = crossingTheBorder("BA").let { base ->
            val day = base.trip.days.first()
            TripContent(
                base.trip.copy(
                    days = listOf(
                        day.copy(
                            // left Croatia in the morning, sleeps in Bosnia
                            cityIds = listOf("zagreb-fixture") + day.cityIds,
                            baseCityId = "sarajevo",
                        ),
                    ),
                    cities = base.trip.cities + base.trip.cities.first().copy(
                        id = "zagreb-fixture",
                        name = "Zagreb",
                        countryCode = "HR",
                        countryName = "Croácia",
                    ),
                ),
                base.assets,
            )
        }

        val state = buildEmergencyState(crossing, date)!!

        assertEquals("Sarajevo · Bósnia e Herzegovina", state.location)
        assertEquals("122", state.police!!.number)
    }

    @Test
    fun `with no profile for this country the first one is the fallback`() {
        val state = buildEmergencyState(
            crossingTheBorder("BA", profiles = listOf(croatia)),
            date,
        )!!

        assertEquals("192", state.police!!.number)
        assertEquals("Sarajevo · Bósnia e Herzegovina", state.location)
    }

    /**
     * The note under the 88dp button is written about one number. The approved
     * sheet says *"Botão único de 88dp 'Ligar 112' com a observação de que
     * funciona sem crédito"* — it ties the sentence to 112, and the real
     * package has a seventh profile where the general number is Brazil's 190,
     * reached on days 1 and 20 (D088).
     */
    @Test
    fun `the note about credit belongs to 112 and to no other number`() {
        val inBosnia = buildEmergencyState(crossingTheBorder("BA"), date)!!
        assertEquals("112", inBosnia.general.number)
        assertTrue(inBosnia.generalWorksWithoutCredit)

        val inBrazil = buildEmergencyState(
            crossingTheBorder("BR", profiles = listOf(brazil) + content.trip.emergencyProfiles),
            date,
        )!!
        assertEquals("190", inBrazil.general.number)
        assertFalse(
            "190 is not free of credit the way 112 is; the sheet's note is about 112",
            inBrazil.generalWorksWithoutCredit,
        )
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

    /**
     * "O hotel onde estão as malas" is tonight's hotel. On a trip with several
     * stays the first one in the package stops being that from the second
     * night on — on the emergency screen (D077).
     */
    @Test
    fun `the hotel is the one the traveller sleeps in tonight`() {
        val trip = TwoCityTrip.content()

        val first = buildEmergencyState(trip, TwoCityTrip.DAY_ONE)!!
        assertEquals("Hospedagem em Sarajevo — exemplo", first.contacts[1].label)

        val second = buildEmergencyState(trip, TwoCityTrip.DAY_TWO)!!
        assertEquals("Hospedagem em Mostar — exemplo", second.contacts[1].label)
        assertEquals(
            "Ligar para a hospedagem, Hospedagem em Mostar — exemplo",
            second.contacts[1].accessibilityLabel,
        )
    }
}
