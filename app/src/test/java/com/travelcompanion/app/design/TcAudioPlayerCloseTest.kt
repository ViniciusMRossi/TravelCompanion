package com.travelcompanion.app.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The compact bar's way out.
 *
 * Added to the approved plate with explicit approval, in the one corner the
 * approved specimen leaves empty: the compact player's head row is the play
 * toggle on the left and a title column that stretches, and nothing has ever
 * been drawn at its trailing end. Nullable like the ±15 buttons, so the full
 * player on screen 09 — which sits on a screen the traveller can leave — is
 * unchanged by its existence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TcAudioPlayerCloseTest {

    @Rule
    @JvmField
    val compose = createComposeRule()

    @Test
    fun `the compact bar closes, and only where the button is passed`() {
        var closed = 0
        compose.setContent {
            Box(modifier = Modifier.requiredWidth(411.dp)) {
                TcAudioPlayer(
                    title = "Audioguia de Baščaršija",
                    subtitle = "Baščaršija",
                    progress = 0.3f,
                    elapsed = "03:36",
                    total = "12:00",
                    isPlaying = false,
                    variant = TcAudioPlayerVariant.Compact,
                    onTogglePlayPause = {},
                    onClose = { closed++ },
                )
            }
        }

        val close = compose.onNodeWithContentDescription("Encerrar audioguia")
        // D153's minimum, the same one the chips were held to.
        close.assertWidthIsAtLeast(48.dp)
        close.assertHeightIsAtLeast(48.dp)

        close.performClick()
        assertEquals(1, closed)
    }

    /**
     * Screen 09's player keeps its own shape: there the audio has a screen of
     * its own, and closing does not mean the same thing.
     */
    @Test
    fun `the full player draws no close button`() {
        compose.setContent {
            Box(modifier = Modifier.requiredWidth(411.dp)) {
                TcAudioPlayer(
                    title = "Capítulo 3 · A esquina de 1914",
                    subtitle = "Caminhada Histórica de Sarajevo",
                    progress = 0.5f,
                    elapsed = "02:10",
                    total = "04:20",
                    isPlaying = true,
                    variant = TcAudioPlayerVariant.Full,
                    onTogglePlayPause = {},
                    onSkipBack = {},
                    onSkipForward = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Encerrar audioguia").assertDoesNotExist()
    }
}
