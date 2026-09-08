package com.travelcompanion.app.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The veil, and the fact that only the photograph gets one.
 *
 * The hero's title is white and bottom-anchored. Against the striped
 * placeholder it always had contrast, because the stripes supply their own,
 * and for the whole of the build in which no photograph shipped that was every
 * hero on screen. The 152 photographs of D166 changed which pixels the title
 * lands on: pale limestone on screen 05's Muralhas, a white hull behind
 * Kotor's `(Suranj)`. Nothing here was wrong until there was a photograph to
 * be wrong against.
 *
 * So the veil goes under the photograph and nowhere else. Darkening the
 * placeholder would alter an approved element that is not broken, which is why
 * the second test is as load-bearing as the first.
 *
 * It carries [TcHeroScrimTag] and not [TcHeroBackdropTag]: `TcHeroGeometryTest`
 * reads the backdrop with `onNodeWithTag`, which demands exactly one node, and
 * a second node wearing that tag would break the D051/D056 guard rather than
 * extend it. The third test here says that in this file too, so the reason
 * travels with the tag.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TcHeroScrimTest {

    @get:Rule
    val compose = createComposeRule()

    private val heroTag = "hero-under-test"

    private fun hero(photograph: ImageBitmap?, caption: String?) {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHeroWith(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .testTag(heroTag),
                    photograph = photograph,
                    placeholderCaption = caption,
                    titleOverPhotograph = true,
                ) {
                    Text(
                        text = "Muralhas da cidade velha",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                    )
                }
            }
        }
    }

    /** With a photograph, there is a layer between it and the title. */
    @Test
    fun `a hero with a photograph carries a veil`() {
        hero(photograph = ImageBitmap(width = 4, height = 4), caption = null)

        compose.onNodeWithTag(TcHeroScrimTag, useUnmergedTree = true).assertIsDisplayed()
    }

    /** And it covers the hero, for D051's reason: it must not size it. */
    @Test
    fun `the veil covers the hero without deciding its height`() {
        hero(photograph = ImageBitmap(width = 4, height = 4), caption = null)

        val hero = compose.onNodeWithTag(heroTag, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(
            "the hero must be the height its own content asked for",
            200.dp,
            hero.bottom - hero.top,
        )
        assertEquals(
            "the veil must be exactly the hero",
            hero,
            compose.onNodeWithTag(TcHeroScrimTag, useUnmergedTree = true).getUnclippedBoundsInRoot(),
        )
    }

    /** With the placeholder, there is not one, and that is deliberate. */
    @Test
    fun `a hero showing the striped placeholder carries no veil`() {
        hero(photograph = null, caption = "foto — teste")

        compose.onNodeWithTag(TcHeroScrimTag, useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * The regression guard of D051 and D056, said here as well.
     *
     * `onNodeWithTag` fails on two matches, so this is what would have caught
     * the veil had it been tagged as another backdrop.
     */
    @Test
    fun `the veil does not become a second backdrop`() {
        hero(photograph = ImageBitmap(width = 4, height = 4), caption = null)

        compose.onNodeWithTag(TcHeroBackdropTag, useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * A photograph with nothing drawn over it gets no veil.
     *
     * Two of the six call sites are shaped like this — screen 01's cover and
     * screen 04's carousel thumbnail, which is 47 images — and they were being
     * darkened by a remedy for a disease they do not have: measured on the
     * same Dubrovnik thumbnail, the top row was identical at 148.3 luminance
     * and the bottom fell from 100.4 to 55.2, about 45% darker, under nothing.
     */
    @Test
    fun `a photograph with no title over it gets no veil`() {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHeroWith(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .testTag(heroTag),
                    photograph = ImageBitmap(width = 4, height = 4),
                    titleOverPhotograph = false,
                )
            }
        }

        compose.onNodeWithTag(TcHeroScrimTag, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag(TcHeroBackdropTag, useUnmergedTree = true).assertIsDisplayed()
    }
}
