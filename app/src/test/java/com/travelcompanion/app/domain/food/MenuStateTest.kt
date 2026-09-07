package com.travelcompanion.app.domain.food

import com.travelcompanion.app.data.trip.Meal
import com.travelcompanion.app.data.trip.TwoCityTrip
import com.travelcompanion.app.domain.today.ShortcutUi
import com.travelcompanion.app.domain.today.TodayUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

/** Screen 20 — Comer aqui. */
class MenuStateTest {

    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    /* ------------------------------------------------------- the day cursor */

    /**
     * The cursor inside screen 20 and the day feeding screen 02's shortcut are
     * two facts that happen to be equal when the screen opens. Walking the
     * cursor must not move the shortcut — if it does, Today starts announcing
     * the food of whatever city the traveller last scrolled to (D089).
     */
    @Test
    fun `walking the day cursor does not move screen 02's shortcut`() {
        val content = MenuFixtures.content(
            sarajevoMenu = MenuFixtures.menu(),
            mostarMenu = MenuFixtures.menu(title = "Sete pratos da Herzegovina", currency = "KM"),
        )
        val today = TodayUseCase(content)

        fun shortcutLabel(): String? = today("vinicius", TwoCityTrip.DAY_ONE, LocalTime.of(9, 0))
            ?.shortcuts
            ?.firstOrNull { it.kind == ShortcutUi.Kind.Food }
            ?.label

        val before = shortcutLabel()
        assertEquals("Comer em Sarajevo", before)

        // Move the cursor to the other city, twice, the way a thumb would.
        val moved = buildFoodState(content, dayIndex = 1, locale = ptBr)
        assertEquals("Comer em Mostar", moved?.header?.title)
        assertEquals("Sete pratos da Herzegovina", moved?.menu?.title)

        assertEquals("the shortcut follows the lived day, not the cursor", before, shortcutLabel())
    }

    @Test
    fun `the cursor starts on the day being lived`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu())

        assertEquals(0, currentDayIndex(content, TwoCityTrip.DAY_ONE))
        assertEquals(1, currentDayIndex(content, TwoCityTrip.DAY_TWO))
    }

    @Test
    fun `the chevrons stop at the ends of the trip`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu())

        val first = buildFoodState(content, dayIndex = 0, locale = ptBr)!!
        assertFalse(first.header.canGoPrevious)
        assertTrue(first.header.canGoNext)

        val last = buildFoodState(content, dayIndex = 1, locale = ptBr)!!
        assertTrue(last.header.canGoPrevious)
        assertFalse(last.header.canGoNext)
    }

    /** The total is the packaged day count, never a literal (the handoff says 21). */
    @Test
    fun `the header counts the days the package actually has`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu())

        val header = buildFoodState(content, dayIndex = 0, locale = ptBr)!!.header
        assertTrue(header.dayLabel, header.dayLabel.startsWith("Dia 9 de 2 · "))
    }

    /* ---------------------------------------------------------- the fallback */

    /**
     * A city with no menu of its own borrows the declared one — and the page it
     * borrows keeps its own country and currency. Inheriting them from the day
     * would file Bosnian food under someone else's cuisine, which is the error
     * the amber notice exists to prevent (D070, D127).
     */
    @Test
    fun `a borrowed menu keeps the country and currency of the menu, not of the day`() {
        val content = MenuFixtures.content(
            sarajevoMenu = MenuFixtures.menu(currency = "KM"),
            mostarMenu = null,
            fallbackMenuCityId = "sarajevo",
        )

        val state = buildFoodState(content, dayIndex = 1, locale = ptBr)!!

        // The header is about the day: the traveller really is in Mostar.
        assertEquals("Comer em Mostar", state.header.title)
        // The notice names both sides, and is not optional.
        assertEquals("Sarajevo", state.borrowedFrom)
        // The body is about the menu.
        assertEquals("Sarajevo", state.menu?.cityName)
        assertEquals("Bósnia e Herzegovina", state.menu?.countryName)
        assertEquals("KM", state.menu?.currency)
        assertNull(state.emptyNote)
    }

    @Test
    fun `a city with its own menu shows no notice`() {
        val content = MenuFixtures.content(
            sarajevoMenu = MenuFixtures.menu(),
            mostarMenu = MenuFixtures.menu(title = "Sete pratos da Herzegovina"),
            fallbackMenuCityId = "sarajevo",
        )

        val state = buildFoodState(content, dayIndex = 1, locale = ptBr)!!

        assertNull(state.borrowedFrom)
        assertEquals("Mostar", state.menu?.cityName)
    }

    /**
     * The fallback is declared, never discovered. With no `fallbackMenuCityId`
     * the screen says so rather than reaching for whichever city happens to sit
     * first in the array — the rule D097 exists to hold.
     */
    @Test
    fun `with no declared fallback the screen says the menu is not written`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu(), mostarMenu = null)

        val state = buildFoodState(content, dayIndex = 1, locale = ptBr)!!

        assertNull(state.menu)
        assertNull(state.borrowedFrom)
        assertEquals(NO_MENU_NOTE, state.emptyNote)
        // The header still works: the traveller is still somewhere on a day.
        assertEquals("Comer em Mostar", state.header.title)
    }

    @Test
    fun `a fallback pointing at a city with no menu is not a fallback`() {
        val content = MenuFixtures.content(
            sarajevoMenu = null,
            mostarMenu = null,
            fallbackMenuCityId = "sarajevo",
        )

        val state = buildFoodState(content, dayIndex = 0, locale = ptBr)!!

        assertNull(state.menu)
        assertEquals(NO_MENU_NOTE, state.emptyNote)
    }

    /* ------------------------------------------------------------ the count */

    /** Summed from the meals. A menu that says ten and carries three is wrong. */
    @Test
    fun `the dish count is derived from the meals`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu())

        assertEquals(3, buildMenuState(content, "sarajevo")!!.dishCount)
    }

    @Test
    fun `the shortcut says one prato in the singular`() {
        val oneDish = MenuFixtures.menu(
            meals = listOf(
                Meal("Almoço", "12:00–15:00", listOf(MenuFixtures.dish("cevapi", "Ćevapi"))),
            ),
        )
        val content = MenuFixtures.content(sarajevoMenu = oneDish)

        val shortcut = buildFoodShortcut(content, TwoCityTrip.DAY_ONE)!!
        assertEquals(1, shortcut.dishCount)
        assertEquals("1 prato", shortcut.trailingNote)
    }

    @Test
    fun `the shortcut pluralises past one`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu())

        assertEquals("3 pratos", buildFoodShortcut(content, TwoCityTrip.DAY_ONE)!!.trailingNote)
    }

    /**
     * The shortcut is a door to the day's own city or it is not there at all.
     * The fallback exists for someone already inside screen 20 walking days,
     * never as a destination Today offers (handoff §1).
     */
    @Test
    fun `no shortcut on a day whose city has no menu, even with a fallback`() {
        val content = MenuFixtures.content(
            sarajevoMenu = MenuFixtures.menu(),
            mostarMenu = null,
            fallbackMenuCityId = "sarajevo",
        )

        assertNotNull(buildFoodShortcut(content, TwoCityTrip.DAY_ONE))
        assertNull(buildFoodShortcut(content, TwoCityTrip.DAY_TWO))
    }

    @Test
    fun `screen 02 draws no food row when the day has no menu`() {
        val content = MenuFixtures.content(sarajevoMenu = null, fallbackMenuCityId = "sarajevo")

        val shortcuts = TodayUseCase(content)("vinicius", TwoCityTrip.DAY_ONE, LocalTime.of(9, 0))!!
            .shortcuts

        assertTrue(shortcuts.none { it.kind == ShortcutUi.Kind.Food })
    }

    /* ------------------------------------------------------------ the cards */

    /**
     * No photograph ships in this build, so every card starts at the dish name.
     * The path is resolved by real presence, not by a promise in the package —
     * the same test screen 05 applies to its hero (D128).
     */
    @Test
    fun `a dish whose photo is not in this build carries no path`() {
        val withPhoto = MenuFixtures.menu(
            meals = listOf(
                Meal(
                    "Almoço",
                    "12:00–15:00",
                    listOf(MenuFixtures.dish("cevapi", "Ćevapi", photoAssetId = "img.cevapi")),
                ),
            ),
        )
        val content = MenuFixtures.content(sarajevoMenu = withPhoto, exists = { false })

        val dish = buildMenuState(content, "sarajevo")!!.meals.single().dishes.single()
        assertNull(dish.photoPath)
        // The brief for the photograph survives even where the photograph does not.
        assertEquals("o prato Ćevapi numa mesa de bairro", dish.photoCaption)
    }

    @Test
    fun `meals keep the order the content declares`() {
        val content = MenuFixtures.content(sarajevoMenu = MenuFixtures.menu())

        assertEquals(
            listOf("Café da manhã", "Almoço"),
            buildMenuState(content, "sarajevo")!!.meals.map { it.name },
        )
    }

    /** A meal with no dish is omitted, never drawn empty (handoff §4). */
    @Test
    fun `an empty meal is omitted`() {
        val withEmpty = MenuFixtures.menu(
            meals = listOf(
                Meal("Café da manhã", "07:00–10:00", listOf(MenuFixtures.dish("burek", "Burek"))),
                Meal("Jantar", "19:00–22:00", emptyList()),
            ),
        )
        val content = MenuFixtures.content(sarajevoMenu = withEmpty)

        assertEquals(
            listOf("Café da manhã"),
            buildMenuState(content, "sarajevo")!!.meals.map { it.name },
        )
    }

    @Test
    fun `a city with no menu builds no menu state`() {
        val content = MenuFixtures.content(sarajevoMenu = null)

        assertNull(buildMenuState(content, "sarajevo"))
        assertNull(buildMenuState(content, "nowhere"))
        assertNull(buildMenuState(content, null))
    }
}
