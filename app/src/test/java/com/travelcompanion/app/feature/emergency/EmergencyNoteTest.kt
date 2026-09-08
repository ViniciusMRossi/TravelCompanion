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
 * out there and that the numbers beside it are what the country publishes.
 * Keeping that in the package and not drawing it puts the explanation out of
 * reach of the moment it is for (D160).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class EmergencyNoteTest {

    @get:Rule
    val compose = createComposeRule()

    private val date: LocalDate = LocalDate.parse("2026-09-21")

    /** The packaged Bosnian profile, with the real note on its 112. */
    private val bosniaNote =
        "O 112 ainda está em implantação na Bósnia; as páginas oficiais do país " +
            "publicam 122, 123 e 124, e são esses que a tela mostra ao lado."

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
