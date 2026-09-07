package com.travelcompanion.app.domain.food

import com.travelcompanion.app.data.trip.City
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One dish card on screen 20. */
data class DishUi(
    val id: String,
    val name: String,
    val pronunciation: String?,
    val priceRange: String?,
    /**
     * Packaged path, or null when no binary reached this build. Null means the
     * card starts at the dish name: there is no photo area and no placeholder,
     * because a striped rectangle over every card would be seventeen apologies
     * (D128).
     */
    val photoPath: String?,
    val photoCaption: String?,
    val description: String,
    val history: String,
    val phrase: String,
    val phraseTranslation: String,
)

/** One meal heading and the dishes under it. */
data class MealUi(
    val name: String,
    val timeRange: String?,
    val dishes: List<DishUi>,
)

/**
 * One city's menu, with the country and currency of *that* city.
 *
 * Nothing here comes from the day being viewed. On a borrowed menu the two
 * differ, and the menu is what labels the page (D127).
 */
data class MenuUiState(
    val cityId: String,
    val cityName: String,
    val countryName: String,
    val title: String,
    val intro: String,
    val currency: String,
    val meals: List<MealUi>,
    val pricesNote: String?,
) {
    /** Summed from the meals, never authored. Screen 02's count reads this. */
    val dishCount: Int get() = meals.sumOf { it.dishes.size }
}

/** The header strip, which is about the day rather than about the food. */
data class FoodHeaderUi(
    val cityName: String,
    val title: String,
    val dayLabel: String,
    val canGoPrevious: Boolean,
    val canGoNext: Boolean,
)

/**
 * Screen 20.
 *
 * Three states, and the third is the one the prototype does not draw: a day
 * whose city has no menu and whose package declares no fallback still has to
 * say something, and it says it in the register of [NO_MENU_NOTE] rather than
 * borrowing a page it was not given (D129).
 */
data class FoodUiState(
    val dayIndex: Int,
    val header: FoodHeaderUi,
    /** City name the menu was borrowed from, or null when it is the day's own. */
    val borrowedFrom: String?,
    val menu: MenuUiState?,
    val emptyNote: String?,
)

/**
 * What screen 02's shortcut says, or null when the day's city has no menu of
 * its own. The fallback is never offered as a destination: it exists for
 * someone already inside screen 20 walking the days (handoff §1).
 */
data class FoodShortcutUi(val cityName: String, val dishCount: Int) {
    val trailingNote: String get() = if (dishCount == 1) "1 prato" else "$dishCount pratos"
    val label: String get() = "Comer em $cityName"
}

/** The sober sentence for a day with no menu and no fallback (D061, D064). */
const val NO_MENU_NOTE: String =
    "O cardápio desta cidade ainda não foi escrito."

/**
 * The city a day happens in, resolved exactly as screens 02, 03 and 17 resolve
 * it: the day's declared base, and only then the first city it lists (D090).
 * Reading anything else lets two screens name different places on one day.
 */
fun cityOfDay(content: TripContent, day: TripDay): City? = content.city(
    day.baseCityId
        // fallback: a day that declares no base still has to name somewhere,
        // and its first city is where it begins (D090).
        ?: day.cityIds.firstOrNull(),
)

/**
 * One city's menu as the screen draws it, or null when that city has none.
 *
 * Takes a city id and not a day on purpose: the body of screen 20 is a
 * property of the menu's city, and the day only decides *which* city id
 * arrives here.
 */
fun buildMenuState(content: TripContent, cityId: String?): MenuUiState? {
    val city = content.city(cityId) ?: return null
    val menu = city.menu ?: return null

    // A meal with no dish is omitted rather than drawn empty (handoff §4). The
    // schema forbids it, so this only catches a package that got past it.
    val meals = menu.meals
        .filter { it.dishes.isNotEmpty() }
        .map { meal ->
            MealUi(
                name = meal.name,
                timeRange = meal.timeRange,
                dishes = meal.dishes.map { dish ->
                    DishUi(
                        id = dish.id,
                        name = dish.name,
                        pronunciation = dish.pronunciation,
                        priceRange = dish.priceRange,
                        // Real presence in this build, not a promise in the
                        // package: the same test screen 05 applies to its hero.
                        photoPath = content.assets.packagedPathIfPresent(dish.photoAssetId),
                        photoCaption = dish.photoCaption,
                        description = dish.description,
                        history = dish.history,
                        phrase = dish.phrase,
                        phraseTranslation = dish.phraseTranslation,
                    )
                },
            )
        }
    if (meals.isEmpty()) return null

    return MenuUiState(
        cityId = city.id,
        cityName = city.name,
        countryName = city.countryName,
        title = menu.title,
        intro = menu.intro,
        currency = menu.currency,
        meals = meals,
        pricesNote = menu.pricesNote,
    )
}

/**
 * Screen 02's food shortcut for a date, or null when it should not appear.
 *
 * **Reads the date and nothing else.** Screen 20 keeps a cursor over the days
 * and that cursor is not a parameter here, because a value that usually equals
 * another is not a guard, it is a coincidence (D089): sharing one would make
 * Today announce the food of whatever day the traveller last scrolled to.
 */
fun buildFoodShortcut(content: TripContent, date: LocalDate): FoodShortcutUi? {
    val day = content.dayFor(date) ?: return null
    val city = cityOfDay(content, day) ?: return null
    // The day's own city only. A borrowed menu is never offered from Today.
    val menu = buildMenuState(content, city.id) ?: return null
    return FoodShortcutUi(cityName = city.name, dishCount = menu.dishCount)
}

/**
 * Screen 20 at one position of its day cursor.
 *
 * [dayIndex] is an index into `content.days`, held by the screen and reset to
 * the current day every time it opens.
 */
fun buildFoodState(content: TripContent, dayIndex: Int, locale: Locale): FoodUiState? {
    val days = content.days
    if (days.isEmpty()) return null
    val index = dayIndex.coerceIn(0, days.lastIndex)
    val day = days[index]
    val dayCity = cityOfDay(content, day)

    val own = buildMenuState(content, dayCity?.id)
    // Declared, never discovered. Scanning for the first city that happens to
    // carry a menu is exactly the shape D097 forbids.
    val borrowed = own ?: buildMenuState(content, content.trip.fallbackMenuCityId)

    return FoodUiState(
        dayIndex = index,
        header = FoodHeaderUi(
            cityName = dayCity?.name.orEmpty(),
            title = "Comer em ${dayCity?.name.orEmpty()}",
            // The total is the packaged day count, never a literal.
            dayLabel = "Dia ${day.dayNumber} de ${days.size} · ${formatDate(day.date, locale)}",
            canGoPrevious = index > 0,
            canGoNext = index < days.lastIndex,
        ),
        borrowedFrom = borrowed?.cityName?.takeIf { own == null },
        menu = borrowed,
        emptyNote = NO_MENU_NOTE.takeIf { borrowed == null },
    )
}

/** Where the cursor starts: the day being lived, or the first day of the trip. */
fun currentDayIndex(content: TripContent, date: LocalDate): Int {
    val today = content.dayFor(date) ?: return 0
    return content.days.indexOfFirst { it.id == today.id }.coerceAtLeast(0)
}

/** "Sexta, 25 de setembro" */
private fun formatDate(isoDate: String, locale: Locale): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val weekday = date.dayOfWeek
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.titlecase(locale) }
        .substringBefore("-")
    val month = date.month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
    return "$weekday, ${date.dayOfMonth} de $month"
}
