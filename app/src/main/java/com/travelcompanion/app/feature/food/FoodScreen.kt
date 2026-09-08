package com.travelcompanion.app.feature.food

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcHairline
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.design.rememberPackagedImage
import com.travelcompanion.app.domain.food.DishUi
import com.travelcompanion.app.domain.food.FoodUiState
import com.travelcompanion.app.domain.food.MealUi
import com.travelcompanion.app.domain.food.MenuUiState

/** 18dp on the dish card, 14 inside it (handoff tokens). */
private val DishCardShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)

/**
 * The gap between the dish name and its price.
 *
 * Named because it is used twice and the two uses have to agree: it is the
 * row's arrangement, and it is subtracted before the price's ceiling is halved.
 */
private val DishHeaderGap = 12.dp

/**
 * The price pill's shape: the plate's stadium while it is one line, and a
 * rounded block once it wraps.
 *
 * `TcPillShape` is `CircleShape`, whose radius is **half the smaller side**. On
 * a one-line price that is half of 30dp — 18sp of line plus 6dp of padding
 * above and below — and it draws the stadium the plate specifies. On a price
 * that wraps to four lines the box is over 100dp tall, the radius becomes 50dp
 * or more, and the corner eats inwards further than the 11dp of horizontal
 * padding: the first glyphs of the top and bottom lines are drawn outside the
 * fill. The telephone found that, on `Peka (carne ou polvo)`, after the ceiling
 * had already fixed the name.
 *
 * So the radius is capped. Below the cap the expression is `minDimension / 2`,
 * character for character what `CircleShape` computes, so every pill that fits
 * on a line is the same pixels it was — including at the larger font scales,
 * where 30dp grows but stays under 48. Above it, the block stops curving.
 */
private val DishPriceShape = RoundedCornerShape(CappedCornerSize(24.dp))

/**
 * `CircleShape`'s corner until it reaches [cap], and [cap] after that.
 *
 * A `Shape` cannot ask how many lines its text took, and it does not need to:
 * a pill that wraps is a pill that grew taller than a line, and the height is
 * what the radius is read from either way. One expression, no branch on
 * content.
 *
 * `internal` for the same reason `TcHeroWith` is: it is the seam a test needs,
 * and the property it has to prove — identical below the cap, clamped above it
 * — is arithmetic, not pixels (D056).
 */
internal data class CappedCornerSize(private val cap: Dp) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        minOf(shapeSize.minDimension / 2f, with(density) { cap.toPx() })
}

/**
 * Screen 20 — Comer aqui.
 *
 * Editorial content inside an offline app: what to eat in the city the
 * traveller is in, why the dish belongs there, and the sentence that orders it.
 * Not a restaurant guide and not a review aggregator — there is no rating, no
 * ranking and no map on this screen by design.
 *
 * The day cursor lives in the caller and is reset to the current day every time
 * the screen opens. It is deliberately not the same value that feeds screen
 * 02's shortcut: a parameter that usually equals another is not a guard, it is
 * a coincidence (D089).
 */
@Composable
fun FoodScreen(
    state: FoodUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        FoodHeader(state, onBack, onPreviousDay, onNextDay)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // First element of the scroll, above the editorial header, so it is
            // read before the food it qualifies (handoff §5).
            state.borrowedFrom?.let { BorrowedNotice(state.header.cityName, it) }

            state.menu?.let { menu ->
                MenuHeader(menu)
                menu.meals.forEach { MealSection(it) }
                menu.pricesNote?.let { note ->
                    Text(note, style = TcType.meta, color = FieldCompanionColors.Neutral600)
                }
            }

            state.emptyNote?.let { note ->
                Text(note, style = TcType.body, color = FieldCompanionColors.Neutral700)
            }
        }
    }
}

/**
 * Three 48dp targets: back on the left, and the two day chevrons on the right.
 *
 * Two different shapes on purpose — the arrow leaves the screen, the chevrons
 * change what is on it. At either end of the trip the corresponding chevron
 * keeps its 48dp of space and stops being a control: greyed, unlabelled and
 * out of the focus order (handoff §1).
 */
@Composable
private fun FoodHeader(
    state: FoodUiState,
    onBack: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(TcPillShape)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "Voltar para Hoje" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                TcIcons.ArrowLeft,
                contentDescription = null,
                tint = FieldCompanionColors.Ink,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.header.title,
                style = TcType.action,
                color = FieldCompanionColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.header.dayLabel,
                style = TcType.label,
                color = FieldCompanionColors.Neutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DayChevron(
            icon = TcIcons.ChevronLeft,
            label = "Dia anterior",
            enabled = state.header.canGoPrevious,
            onClick = onPreviousDay,
        )
        DayChevron(
            icon = TcIcons.ChevronRight,
            label = "Próximo dia",
            enabled = state.header.canGoNext,
            onClick = onNextDay,
        )
    }
}

/**
 * A day chevron in both of its states.
 *
 * Disabled is not "the same button, dimmed": it is not a button at all. It
 * keeps the 48dp so the header does not reflow at the ends of the trip, and it
 * is hidden from accessibility so a screen reader is not offered a day that
 * does not exist.
 */
@Composable
private fun DayChevron(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .then(
                if (enabled) {
                    Modifier
                        .clip(TcPillShape)
                        .clickable(onClick = onClick)
                        .semantics { contentDescription = label }
                } else {
                    Modifier.semantics { hideFromAccessibility() }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) FieldCompanionColors.Ink else FieldCompanionColors.Neutral400,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The amber note that says whose food this is (handoff §5). Never a toast. */
@Composable
private fun BorrowedNotice(dayCityName: String, menuCityName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.GoldSoft)
            .border(1.dp, FieldCompanionColors.GoldBorder, TcCardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            TcIcons.Warning,
            contentDescription = null,
            tint = FieldCompanionColors.GoldInk,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Cardápio próprio de $dayCityName ainda não escrito. " +
                "Mostrando os pratos de $menuCityName como referência.",
            style = TcType.meta,
            color = FieldCompanionColors.GoldBody,
        )
    }
}

/** Country and currency come from the menu on screen, never from the day (D127). */
@Composable
private fun MenuHeader(menu: MenuUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                TcIcons.Fork,
                contentDescription = null,
                tint = FieldCompanionColors.Teal,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "CULINÁRIA LOCAL · ${menu.countryName.uppercase()}",
                style = TcType.eyebrow,
                color = FieldCompanionColors.Neutral600,
            )
        }
        Text(menu.title, style = TcType.heroEditorial, color = FieldCompanionColors.Ink)
        Text(menu.intro, style = TcType.body, color = FieldCompanionColors.Neutral700)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                TcIcons.Offline,
                contentDescription = null,
                tint = FieldCompanionColors.Teal,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Preços em ${menu.currency} · esta página fica offline",
                style = TcType.meta,
                color = FieldCompanionColors.Neutral600,
            )
        }
    }
}

@Composable
private fun MealSection(meal: MealUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = meal.name,
                    style = TcType.mealTitle,
                    color = FieldCompanionColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                meal.timeRange?.let {
                    Text(it, style = TcType.metaTabular, color = FieldCompanionColors.Neutral600)
                }
            }
            TcHairline()
        }
        meal.dishes.forEach { DishCard(it) }
    }
}

@Composable
private fun DishCard(dish: DishUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DishCardShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, DishCardShape),
    ) {
        // All seventy-five dishes now package a photograph (D166), so this is
        // drawn. The `let` stays as the contract it always was: a dish without
        // one starts at the name, with no empty photo area and no placeholder,
        // because striped rectangles here would be apologies (D128).
        dish.photoPath?.let { DishPhoto(it, dish.photoCaption) }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // The price pill is measured against a ceiling: half of what it
            // and the name have between them.
            //
            // `priceRange` is the source's own sentence, not a string the app
            // composed from a number, so nothing bounds its length: 68 of the
            // 75 dishes are under twenty characters, and `Peka (carne ou
            // polvo)` carries sixty-five. Left unweighted in a `Row`, the pill
            // measured first and at whatever width it liked, and the name's
            // `weight(1f)` got the remainder — which for sixty-five characters
            // was almost nothing, so `Peka (carne ou polvo)` came out one
            // letter per line. That is D154's defect in the family D154 missed,
            // because the field is `priceRange` and D154 searched `price`.
            //
            // The remedy is not a length test. "Past N characters, draw it
            // differently" is a branch whose condition agrees with the intent
            // almost always, which is D089's shape. The rule here is one
            // constraint applied to all seventy-five: **a qualifier may not
            // take more room than the thing it qualifies** — D055's clause,
            // measured. An even split is where that turns over. The pill wraps
            // inside the ceiling instead of truncating, because the half of
            // these sentences that an ellipsis eats is the instruction —
            // "mínimo 2 pessoas", "encomendar com antecedência", "vendido por
            // kg" — which is exactly what D154 found on screen 05.
            //
            // The sixty-eight short ones never reach the ceiling, so they
            // measure exactly as they did and the name keeps exactly the width
            // it had. `€2–3` is untouched.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Half of what the two of them actually have, which is the row
                // less the gap between them — so at the ceiling the pill and
                // the name come out equal, and the qualifier never ends up the
                // wider of the two.
                val priceCeiling = (maxWidth - DishHeaderGap) / 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DishHeaderGap),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = dish.name,
                            style = TcType.dishName,
                            color = FieldCompanionColors.Ink,
                        )
                        dish.pronunciation?.let {
                            Text(
                                text = it,
                                style = TcType.pronunciation,
                                color = FieldCompanionColors.Neutral600,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                    dish.priceRange?.let {
                        Text(
                            text = it,
                            style = TcType.priceTabular,
                            color = FieldCompanionColors.MossInk,
                            modifier = Modifier
                                .widthIn(max = priceCeiling)
                                .clip(DishPriceShape)
                                .background(FieldCompanionColors.MossSoft)
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Text(dish.description, style = TcType.body, color = FieldCompanionColors.Ink)
            Text(dish.history, style = TcType.historyBody, color = FieldCompanionColors.Neutral700)

            // Nothing to order, so nothing to say: the card ends at the
            // history. An empty teal box, or one carrying invented words,
            // would both be worse than its absence (D130).
            if (dish.phrase != null && dish.phraseTranslation != null) {
                OrderBlock(dish.phrase, dish.phraseTranslation)
            }
        }
    }
}

/**
 * A packaged photograph, when one exists. The card clips it, so it carries no
 * radius of its own.
 */
@Composable
private fun DishPhoto(path: String, caption: String?) {
    val bitmap = rememberPackagedImage(path) ?: return
    Image(
        bitmap = bitmap,
        contentDescription = caption,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp),
    )
}

/**
 * "Para pedir": selectable and copyable, and nothing else. Not a button, does
 * not speak, does not open a translator — all three are out of scope for this
 * delivery, and a box that looks pressable but is not would be worse than a
 * box that plainly is not.
 */
@Composable
private fun OrderBlock(phrase: String, translation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.TealSoft)
            .border(1.dp, FieldCompanionColors.TealBorder, TcCardShape)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("PARA PEDIR", style = TcType.eyebrowSmall, color = FieldCompanionColors.Teal)
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(phrase, style = TcType.orderPhrase, color = FieldCompanionColors.Ink)
                Text(
                    text = translation,
                    style = TcType.meta,
                    color = FieldCompanionColors.TealDark,
                )
            }
        }
    }
}
