package com.travelcompanion.app.feature.food

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.domain.food.DishUi
import com.travelcompanion.app.domain.food.FoodHeaderUi
import com.travelcompanion.app.domain.food.FoodUiState
import com.travelcompanion.app.domain.food.MealUi
import com.travelcompanion.app.domain.food.MenuUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The dish name is not allowed to be crushed by the price beside it.
 *
 * `priceRange` is the source's own sentence, not a string the app composed
 * from a number, so nothing bounds its length. The pill was an unweighted
 * `Text` in a `Row` whose only other child carried `weight(1f)`, and Compose
 * measures the unweighted child first, at whatever width it asks for: for
 * `Peka (carne ou polvo)`'s sixty-five characters that was nearly the whole
 * row, leaving the name a column about one character wide. On the telephone
 * the name came out one letter per line, down the side of the card.
 *
 * That is D154's defect in the family D154 missed — D154 searched `price` and
 * `priceNote`, found no dish prices, and left screen 20 alone, because the
 * field is `priceRange`.
 *
 * Both halves are asserted here, because the fix is only worth having if it
 * leaves the other sixty-eight alone: the long one must give way, and the
 * short one must still absorb the whole remainder rather than be pinned to
 * some tidy half. No test here counts characters — the rule under test is one
 * layout constraint applied to all seventy-five, not a branch that turns on
 * length (D089).
 *
 * The content is composed inside a `requiredWidth` box because this rule's
 * host has no width of its own — it measures 0.5dp — and every number below
 * is a width. `requiredWidth` and not `width`, because the plain one is
 * clamped by the incoming constraints and would collapse with the host.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
// Every assertion here is a width, and the legacy graphics shadow cannot
// measure text: under it every `Text` in the screen comes out about one
// character wide, the long price and the short one alike, so the suite would
// be green on a measurement that means nothing.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DishPriceLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    /** The real string, character for character, from the shipping package. */
    private val peka65 = "€25–35 por pessoa (mínimo 2 pessoas, encomendar com antecedência)"

    /** The real string two cards later, which must not move. */
    private val kava4 = "€2–3"

    /** 411 screen − 20+20 screen padding − 16+16 card padding. */
    private val cardContent = 339.dp

    private fun dish(name: String, price: String) = DishUi(
        id = "dish.test",
        name = name,
        pronunciation = null,
        priceRange = price,
        photoPath = null,
        photoCaption = null,
        description = "Uma descrição curta, para o cartão ter corpo.",
        history = "Uma história curta.",
        phrase = null,
        phraseTranslation = null,
    )

    private fun screen(name: String, price: String) {
        val state = FoodUiState(
            dayIndex = 15,
            header = FoodHeaderUi(
                cityName = "Dubrovnik",
                title = "Comer em Dubrovnik",
                dayLabel = "Dia 16 de 20",
                canGoPrevious = true,
                canGoNext = true,
            ),
            borrowedFrom = null,
            menu = MenuUiState(
                cityId = "dubrovnik",
                cityName = "Dubrovnik",
                countryName = "Croácia",
                title = "Nove pratos da antiga república marítima",
                intro = "A mesa de Dubrovnik é dálmata.",
                currency = "EUR",
                meals = listOf(
                    MealUi(
                        name = "Jantar",
                        timeRange = "19:00 – 22:00",
                        dishes = listOf(dish(name, price)),
                    ),
                ),
                pricesNote = null,
            ),
            emptyNote = null,
        )
        compose.setContent {
            Box(modifier = Modifier.requiredWidth(411.dp).requiredHeight(891.dp)) {
                FoodScreen(state = state, onBack = {}, onPreviousDay = {}, onNextDay = {})
            }
        }
    }

    private fun widthOf(text: String): Dp =
        compose.onNodeWithText(text, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
            .let { it.right - it.left }

    /**
     * The sixty-five-character price, and the name beside it.
     *
     * The assertion is D055's clause turned into a measurement: a qualifier
     * may not take more room than the thing it qualifies. Before the fix the
     * pill took nearly the whole row and the name was left about one character
     * wide, so both halves of this were red.
     */
    @Test
    fun `a sixty-five character price leaves the dish name a legible column`() {
        screen("Peka (carne ou polvo)", peka65)

        val name = widthOf("Peka (carne ou polvo)")
        val price = widthOf(peka65)

        assertTrue(
            "the name must be at least as wide as the price that qualifies it, " +
                "but the name was $name and the price $price",
            name.value >= price.value,
        )
        assertTrue(
            "the name column must not be crushed to a letter, but it was $name",
            name > cardContent / 3,
        )
    }

    /** And the whole sentence is laid out: the instruction is the half an ellipsis eats. */
    @Test
    fun `the whole sixty-five characters are laid out, not truncated`() {
        screen("Peka (carne ou polvo)", peka65)

        compose.onNodeWithText(peka65, useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        val bounds = compose.onNodeWithText(peka65, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val height = bounds.bottom - bounds.top
        assertTrue(
            "sixty-five characters inside half a card must wrap onto several " +
                "lines rather than be cut to one, but the pill was $height tall",
            height > 40.dp,
        )
    }

    /**
     * The companion, and the reason the fix is a ceiling rather than a split.
     *
     * `€2–3` never reaches the ceiling, so the pill measures exactly what it
     * always measured and the name still takes the entire remainder. Had the
     * price been given a weight instead — the obvious fix — the name would
     * have been pinned to half the card here and this assertion would fail,
     * which is the whole reason it is written as a floor above the half.
     */
    @Test
    fun `a four-character price is untouched and the name still takes the rest`() {
        screen("Kava na Stradun", kava4)

        val name = widthOf("Kava na Stradun")
        val price = widthOf(kava4)

        assertTrue(
            "the short pill must stay small, but it was $price",
            price < cardContent / 4,
        )
        assertTrue(
            "the name must still absorb the whole remainder rather than be " +
                "pinned to half the card — half is ${cardContent / 2}, measured $name",
            name > cardContent / 2,
        )
    }
}
