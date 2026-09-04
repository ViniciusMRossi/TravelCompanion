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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
 * This is the guard for that, and deliberately only that: it composes the hero
 * in the shape that broke — content-driven height, unbounded parent — and asks
 * whether the backdrop covers it. It is not a screenshot suite and has no
 * golden images (D056).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TcHeroGeometryTest {

    @get:Rule
    val compose = createComposeRule()

    /** Screen 05's shape: `heightIn(min = …)` inside a vertical scroll. */
    @Test
    fun `the backdrop covers a hero whose height comes from its own content`() {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHero(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
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

        compose.onNodeWithTag(TcHeroBackdropTag).assertHeightIsAtLeast(200.dp)
    }

    /** And the fixed-height shape screens 01 and 09 use, which never broke. */
    @Test
    fun `the backdrop covers a hero given a fixed height`() {
        compose.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TcHero(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 262.dp, max = 262.dp),
                    placeholderCaption = "foto — teste",
                )
            }
        }

        compose.onNodeWithTag(TcHeroBackdropTag).assertHeightIsAtLeast(262.dp)
    }
}
