package com.travelcompanion.app.feature.emergency

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.travelcompanion.app.data.trip.EmergencyContact
import com.travelcompanion.app.data.trip.EmergencyProfile
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * What screen 17 asks the dialer for.
 *
 * This is the assertion that replaces pressing the button: no test, on any
 * device or emulator, completes a call to a real emergency number. The screen
 * is composed here, the button is pressed here, and what is checked is the
 * number handed to the launcher — which opens `ACTION_DIAL` with the number
 * filled in and stops (D064).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencyDialTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Two countries in the package, the wrong one first. What is pressed here
     * has to be Bosnia's, because that is where the day puts the traveller —
     * with a single profile in the fixture this test's own name was a claim it
     * could not make (D070).
     */
    private val content = packagedContent(exists = { false }).let { packaged ->
        val croatia = EmergencyProfile(
            id = "emergency.hr",
            countryCode = "HR",
            countryName = "Croácia",
            generalEmergency = EmergencyContact(label = "Emergência geral", phone = "112"),
            police = EmergencyContact(label = "Polícia", phone = "192"),
            ambulance = EmergencyContact(label = "Ambulância", phone = "194"),
        )
        TripContent(
            packaged.trip.copy(
                emergencyProfiles = listOf(croatia) + packaged.trip.emergencyProfiles,
            ),
            packaged.assets,
        )
    }

    private val state = buildEmergencyState(content, LocalDate.parse("2026-09-21"))!!

    private val dialled = mutableListOf<String>()

    private fun screen() {
        compose.setContent {
            EmergencyScreen(state = state, onBack = {}, onDial = { dialled += it })
        }
    }

    @Test
    fun `the primary button asks the dialer for 112 and nothing else`() {
        screen()

        compose.onNodeWithContentDescription("Ligar para Emergência geral").performClick()

        assertEquals(listOf("112"), dialled)
    }

    @Test
    fun `police and ambulance carry the numbers of this country`() {
        screen()

        compose.onNodeWithContentDescription("Ligar para Polícia").performClick()
        compose.onNodeWithContentDescription("Ligar para Ambulância").performClick()

        assertEquals(listOf("122", "124"), dialled)
    }

    /**
     * A row whose number the package has not filled in yet is still drawn — it
     * says the number is coming — but pressing it must not reach the dialer.
     */
    @Test
    fun `a withheld contact is not a button`() {
        screen()

        compose.onNodeWithText("Seguro viagem").performClick()
        compose.onNodeWithText("Representação brasileira").performClick()

        assertEquals(emptyList<String>(), dialled)
    }
}
