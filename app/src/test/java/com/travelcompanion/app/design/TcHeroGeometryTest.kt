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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The one thing about a hero that no state assertion can see.
 *
 * Screen 05's hero drew nothing for a whole phase: its backdrop was measured
 * with `fillMaxSize`, which resolves to zero when the incoming maximum height
 * is infinite — and a hero whose height comes from its own content sits inside
 * a scrolling column, where it is. The card's surface showed through and the
 * white title on top of it was invisible. Every unit test passed; a person
 * looking at the screen found it (D051).
 *
 * The assertion has two halves, and it took two tries to get there. "At least
 * as tall as the declared minimum" was the first, and a broken `Image` sailed
 * past it: it does not measure to nothing, it measures to its painter's aspect
 * ratio, which for a square painter in a full-width hero came to 320dp.
 * Equality between backdrop and hero was the second, and that passed too — a
 * backdrop measured with `fillMaxSize` still *sizes* its parent, so the hero
 * inflated with it and the two were equal and both wrong. So the hero's own
 * height is pinned as well (D056).
 *
 * It is not a screenshot suite and has no golden images.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// One case composes a real `ImageBitmap`, which the legacy graphics shadow
// cannot allocate — it hands back a null Bitmap.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TcHeroGeometryTest {

    @get:Rule
    val compose = createComposeRule()

    private val heroTag = "hero-under-test"

    private fun bounds(tag: String): DpRect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    /**
     * Both halves of "covers the hero without deciding it".
     *
     * Equality alone is not enough: a backdrop measured with `fillMaxSize`
     * still sizes its parent, so a broken `Image` inflates the hero to its
     * painter's aspect ratio and the two grow together, equal and both wrong.
     * So the hero's own height is pinned as well — it must be what its content
     * and its constraints asked for, not what the backdrop wanted.
     */
    private fun assertBackdropCoversHero(expectedHeroHeight: Dp) {
        val hero = bounds(heroTag)
        assertEquals(
            "the hero must be the height its own content asked for",
            expectedHeroHeight,
            hero.bottom - hero.top,
        )
        assertEquals("the backdrop must be exactly the hero", hero, bounds(TcHeroBackdropTag))
    }

    /** Screen 05's shape: `heightIn(min = …)` inside a vertical scroll. */
    @Test
    fun `the backdrop covers a hero whose height comes from its own content`() {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHeroWith(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .testTag(heroTag),
                    placeholderCaption = "foto — teste",
                ) {
                    Text(
                        text = "Baščaršija",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                    )
                }
            }
        }

        assertBackdropCoversHero(expectedHeroHeight = 200.dp)
    }

    /**
     * The same shape, with a photograph in it.
     *
     * The placeholder branch is the only one `TcHero` can reach today, because
     * no photograph ships in this build — so a guard that composes `TcHero`
     * leaves the `Image` branch untested, and that is the branch that becomes
     * the live one the day real photography lands, which Phase 1 still lists
     * as open. `TcHeroWith` takes the photograph already resolved so it can be
     * measured without one being packaged.
     */
    @Test
    fun `the backdrop covers a hero that has a photograph`() {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHeroWith(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .testTag(heroTag),
                    photograph = ImageBitmap(width = 4, height = 4),
                ) {
                    Text(
                        text = "Baščaršija",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                    )
                }
            }
        }

        assertBackdropCoversHero(expectedHeroHeight = 200.dp)
    }

    /** And the fixed-height shape screens 01 and 09 use, which never broke. */
    @Test
    fun `the backdrop covers a hero given a fixed height`() {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHeroWith(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 262.dp, max = 262.dp)
                        .testTag(heroTag),
                    placeholderCaption = "foto — teste",
                )
            }
        }

        assertBackdropCoversHero(expectedHeroHeight = 262.dp)
    }
}
