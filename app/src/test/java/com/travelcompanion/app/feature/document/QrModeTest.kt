package com.travelcompanion.app.feature.document

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QR mode's window behaviour, and the half of it that gets forgotten.
 *
 * Brief §18 asks for full brightness and a screen that stays awake — and then
 * for the previous behaviour to be *restored* on the way out. The restoration
 * is what a person testing by hand tends not to check, and it is the part that
 * leaves a phone burning its battery at 100% brightness afterwards (D062).
 *
 * A camera reading a real code is a different verification and it needs real
 * ticket data to point at (D061). This is the part that can be proved here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QrModeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun brightness(): Float = compose.activity.window.attributes.screenBrightness

    private fun keepsScreenOn(): Boolean =
        compose.activity.window.attributes.flags and
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

    @Test
    fun `opening the code raises brightness and keeps the screen awake, closing puts both back`() {
        val before = brightness()

        compose.setContent {
            var showing by remember { mutableStateOf(false) }
            if (showing) {
                BrightScreen()
                Text("fechar", modifier = Modifier.clickable { showing = false })
            } else {
                Text("abrir", modifier = Modifier.clickable { showing = true })
            }
        }

        compose.onNodeWithText("abrir").performClick()
        compose.waitForIdle()

        assertEquals(
            "the code has to be readable across a counter",
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL,
            brightness(),
        )
        assertTrue("a screen that sleeps mid-queue is a screen that failed", keepsScreenOn())

        compose.onNodeWithText("fechar").performClick()
        compose.waitForIdle()

        assertEquals("brightness must go back to what it was", before, brightness())
        assertTrue("and the screen must be allowed to sleep again", !keepsScreenOn())
    }

    /**
     * The gate, from the other side: real ticket data does become a code.
     *
     * The packaged trip is `isMockContent`, so this builds the same content
     * with the flag cleared rather than touching the package (D043, D061).
     */
    @Test
    fun `real ticket data becomes a code`() {
        val packaged = packagedContent(exists = { true })
        val real = TripContent(
            packaged.trip.copy(metadata = packaged.trip.metadata.copy(isMockContent = false)),
            packaged.assets,
        )

        val state = buildDocumentState(real, "ticket.sarajevo-mostar")!!
        assertEquals(QrState.Generated("MOCK-ABC123"), state.qr)
    }
}
