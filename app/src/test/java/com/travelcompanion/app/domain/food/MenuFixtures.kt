package com.travelcompanion.app.domain.food

import com.travelcompanion.app.data.trip.Dish
import com.travelcompanion.app.data.trip.Meal
import com.travelcompanion.app.data.trip.Menu
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TwoCityTrip

/**
 * Menus built in Kotlin, never in a package.
 *
 * No dish is written into `assets/` or `trip-package/` by these tests: the
 * seventeen real dishes are content and arrive in their own session. What the
 * screen needs to be tested against is *shape* — a city with a menu, a city
 * without one, a declared fallback and no fallback at all — and shape is
 * cheaper and clearer here than in a package nobody would ship.
 *
 * Built on [TwoCityTrip] because it is the only fixture in which "the first
 * element" and "the element for today" are different answers (D070, D077).
 */
object MenuFixtures {

    fun dish(id: String, name: String, photoAssetId: String? = null) = Dish(
        id = id,
        name = name,
        pronunciation = "/$id/",
        priceRange = "3–5 KM",
        photoAssetId = photoAssetId,
        photoCaption = "o prato $name numa mesa de bairro",
        description = "O que chega à mesa.",
        history = "Por que este prato é daqui.",
        phrase = "Jedan $name, molim.",
        phraseTranslation = "Um $name, por favor.",
    )

    /**
     * A dish nobody orders: the half-board table at the Bastasi rafting camp,
     * which arrives served and has no sentence to say to anyone (D130).
     */
    fun servedDish(id: String, name: String) = Dish(
        id = id,
        name = name,
        priceRange = "incluído na meia pensão",
        description = "Mesa farta de meia pensão, servida em mesa comunitária.",
        history = "Cardápio fixo do camp de rafting, para quem vai remar o dia inteiro.",
    )

    fun menu(
        title: String = "Dez pratos da mesa bósnia",
        currency: String = "KM",
        meals: List<Meal> = listOf(
            Meal("Café da manhã", "07:00–10:00", listOf(dish("burek", "Burek"))),
            Meal("Almoço", "12:00–15:00", listOf(dish("cevapi", "Ćevapi"), dish("klepe", "Klepe"))),
        ),
        pricesNote: String? = "Faixas observadas em setembro de 2026.",
    ) = Menu(
        title = title,
        intro = "O que caracteriza a cozinha daqui.",
        currency = currency,
        pricesNote = pricesNote,
        meals = meals,
    )

    /**
     * Day 1 is Sarajevo and day 2 is Mostar. [sarajevoMenu] and [mostarMenu]
     * are attached only when given, so a test can ask for exactly the shape it
     * is about.
     */
    fun content(
        sarajevoMenu: Menu? = null,
        mostarMenu: Menu? = null,
        fallbackMenuCityId: String? = null,
        exists: (String) -> Boolean = { false },
    ): TripContent {
        val base = TwoCityTrip.content(exists)
        val cities = base.trip.cities.map { city ->
            when (city.id) {
                "mostar" -> city.copy(menu = mostarMenu)
                else -> city.copy(menu = sarajevoMenu)
            }
        }
        return TripContent(
            base.trip.copy(cities = cities, fallbackMenuCityId = fallbackMenuCityId),
            base.assets,
        )
    }
}
