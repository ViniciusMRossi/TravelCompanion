package com.travelcompanion.app.feature.city

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.travelcompanion.app.data.trip.EditorialSection
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * `city.historySections` on screen 04.
 *
 * The attraction-shaped twin of this field has been drawn since D133; the
 * city's had the same shape in the schema and fell on the floor. Nothing in
 * the package fills it today — 0 of 19 — so the second test here is the one
 * that matters for this build: an empty field draws no heading and no gap,
 * which is the same rule as the missing photograph (D157).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class CityEditorialSectionsTest {

    @get:Rule
    val compose = createComposeRule()

    private val date: LocalDate = LocalDate.parse("2026-09-21")

    private val sections = listOf(
        EditorialSection(
            title = "A cidade sob o império",
            body = "O primeiro parágrafo da primeira seção.\n\nO segundo parágrafo da primeira.",
        ),
        EditorialSection(
            title = "O cerco e o que ficou",
            body = "O primeiro parágrafo da segunda seção.\n\nO segundo parágrafo da segunda.",
        ),
    )

    private fun content(withSections: List<EditorialSection>): TripContent {
        val packaged = packagedContent(exists = { false })
        return TripContent(
            packaged.trip.copy(
                cities = packaged.trip.cities.map { it.copy(historySections = withSections) },
            ),
            packaged.assets,
        )
    }

    private fun screen(content: TripContent) {
        val state = buildCityState(content, content.trip.cities.first().id, date)!!
        compose.setContent {
            CityScreen(
                state = state,
                onBack = null,
                onPlayGuide = {},
                onSeekToChapter = {},
                onOpenAttraction = {},
                onStartWalk = {},
                onPlayStory = {},
                onOpenAction = {},
                cities = emptyList(),
                onSelectCity = {},
            )
        }
    }

    @Test
    fun `both titles and every paragraph reach the screen, kept apart`() {
        screen(content(sections))

        listOf(
            "A cidade sob o império",
            "O primeiro parágrafo da primeira seção.",
            "O segundo parágrafo da primeira.",
            "O cerco e o que ficou",
            "O primeiro parágrafo da segunda seção.",
            "O segundo parágrafo da segunda.",
        ).forEach { text ->
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
        }
    }

    /**
     * All 19 packaged cities are in this state, so this is the assertion that
     * nothing moved for anybody in this build.
     */
    @Test
    fun `a city with no sections draws no empty block`() {
        screen(content(emptyList()))

        compose.onNodeWithText("A cidade sob o império").assertDoesNotExist()
        // The editorial layer either side of it is untouched.
        compose.onNodeWithText("O que ver".uppercase()).performScrollTo().assertIsDisplayed()
    }
}
