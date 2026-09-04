package com.travelcompanion.app.feature.city

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * What the chapter list actually asks the player for, and what a long pill
 * does to the card that holds it.
 *
 * `seekToChapter` has been implemented and tested since Phase 2 with no
 * approved surface to drive it (D025). This is the surface, so this is where
 * the index it is given stops being an assumption.
 */
@RunWith(RobolectricTestRunner::class)
// Robolectric's default window is 320 x 470 px, which puts most of this
// screen below the fold — and a tap outside the window is dropped in silence
// rather than failing, so the first version of these tests "passed" a click
// that never happened. A phone-sized window is what makes them real.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class CityChapterTest {

    @get:Rule
    val compose = createComposeRule()

    private val state = buildCityState(
        packagedContent(exists = { false }),
        "sarajevo",
        LocalDate.parse("2026-09-21"),
    )!!

    private val sought = mutableListOf<Int>()

    private fun screen(state: CityUiState = this.state) {
        compose.setContent {
            CityScreen(
                state = state,
                onBack = null,
                onPlayGuide = {},
                onSeekToChapter = { sought += it },
                onOpenAttraction = {},
                onStartWalk = {},
                onPlayStory = {},
                onOpenAction = {},
            )
        }
    }

    @Test
    fun `a chapter asks for its own index, not the number on its row`() {
        screen()

        compose.onNode(hasClickAction() and hasText("Capítulos")).performClick()
        val third = state.guide!!.chapters[2]
        compose.onNodeWithText(third.title, useUnmergedTree = true).performClick()

        // The row reads "3" and the player is asked for index 2.
        assertEquals(listOf(2), sought)
    }

    @Test
    fun `the chapters are not in the way until they are asked for`() {
        screen()

        val first = state.guide!!.chapters.first().title
        compose.onNodeWithText(first, useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasClickAction() and hasText("Capítulos")).performClick()
        compose.onNodeWithText(first, useUnmergedTree = true).assertExists()
    }

    /**
     * "Ler" opens the story where it stands: screen 10 is the canonical
     * reading surface and is not in this block.
     */
    @Test
    fun `a story opens and closes in place`() {
        screen()
        val story = state.stories.first()

        compose.onNodeWithText(story.body, useUnmergedTree = true).assertDoesNotExist()
        // The stories are the last section of the longest screen in the app,
        // so the button has to be brought into the window before it can be
        // pressed — a tap outside the window is dropped silently.
        compose.onAllNodes(hasClickAction() and hasText("Ler"))[0]
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(story.body, useUnmergedTree = true).assertExists()
        compose.onNode(hasClickAction() and hasText("Fechar")).performScrollTo().performClick()
        compose.onNodeWithText(story.hook, useUnmergedTree = true).assertExists()
    }
}
