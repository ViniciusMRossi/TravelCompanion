package com.travelcompanion.app.feature.emergency

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.travelcompanion.app.data.trip.EmergencyContact
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The sentence under the general emergency number.
 *
 * Three of the seven packaged profiles carry `generalEmergency.note`, and it is
 * operational rather than editorial: Bosnia's says 112 is still being rolled
 * out there, names the police and the ambulance that this screen does dial,
 * and names the fire brigade as the one it does not. Keeping that in the
 * package and not drawing it puts the explanation out of reach of the moment
 * it is for (D160).
 *
 * The note is injected rather than read off the package, so this test cannot
 * tell when the real sentence changes underneath it — which is exactly what
 * happened once (D163). The constant below is kept word for word identical to
 * the packaged one for that reason.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class EmergencyNoteTest {

    @get:Rule
    val compose = createComposeRule()

    private val date: LocalDate = LocalDate.parse("2026-09-21")

    /**
     * The packaged Bosnian note, word for word.
     *
     * Every clause of it is checkable against the screen this test composes:
     * 122 and 124 are drawn immediately below, and 123 is named as a number
     * this screen does not dial — which is why it can be trusted the next time
     * somebody reads it (D163).
     */
    private val bosniaNote =
        "O 112 ainda está em implantação na Bósnia. A polícia é 122 e a ambulância " +
            "124, logo abaixo; os bombeiros são 123, que esta tela não disca."

    private fun contentWithNote(note: String?): TripContent {
        val packaged = packagedContent(exists = { false })
        return TripContent(
            packaged.trip.copy(
                emergencyProfiles = packaged.trip.emergencyProfiles.map { profile ->
                    profile.copy(
                        generalEmergency = EmergencyContact(
                            label = profile.generalEmergency.label,
                            phone = profile.generalEmergency.phone,
                            note = note,
                        ),
                    )
                },
            ),
            packaged.assets,
        )
    }

    private fun screen(content: TripContent) {
        val state = buildEmergencyState(content, date)!!
        compose.setContent {
            EmergencyScreen(state = state, onBack = {}, onDial = {})
        }
    }

    @Test
    fun `the note is drawn under the general emergency number`() {
        screen(contentWithNote(bosniaNote))

        compose.onNodeWithText(bosniaNote).performScrollTo().assertIsDisplayed()
    }

    /** Four of the seven profiles carry none, and draw nothing in its place. */
    @Test
    fun `a profile with no note draws nothing where it would go`() {
        screen(contentWithNote(null))

        compose.onNodeWithText(bosniaNote).assertDoesNotExist()
        // The block itself is untouched: the button and its own sentence stay.
        compose.onNodeWithText("Ligar 112").assertIsDisplayed()
        compose.onNodeWithText("Funciona sem crédito e sem chip local.").assertIsDisplayed()
    }
}
