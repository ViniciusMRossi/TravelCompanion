package com.travelcompanion.app.data.trip

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import java.time.LocalDate

/**
 * The packaged trip with a second city, a second night and a second day.
 *
 * The package ships one of everything, which is exactly the shape in which
 * "the first element of the array" and "the element for today" are the same
 * answer — so tests written against it could not tell the two apart, and three
 * screens shipped reading the first element (D070, D077). Anything that
 * resolves something *for a day* is tested against this instead.
 *
 * Day 9 is Sarajevo, day 10 is Mostar.
 */
object TwoCityTrip {

    val DAY_ONE: LocalDate = LocalDate.parse("2026-09-21")
    val DAY_TWO: LocalDate = LocalDate.parse("2026-09-22")

    fun content(exists: (String) -> Boolean = { false }): TripContent {
        val packaged = packagedContent(exists)
        val trip = packaged.trip
        val sarajevo = trip.cities.single()
        val firstStay = trip.accommodations.single()
        val firstDay = trip.days.single()

        val mostar = sarajevo.copy(
            id = "mostar",
            name = "Mostar",
            intro = "A segunda cidade, para os testes que precisam de duas.",
            attractionIds = emptyList(),
            walkIds = emptyList(),
            storyIds = emptyList(),
            restaurants = emptyList(),
            audioGuideId = null,
        )
        val mostarStay = firstStay.copy(
            id = "stay.mostar",
            name = "Hospedagem em Mostar — exemplo",
            cityId = mostar.id,
        )

        return TripContent(
            trip.copy(
                cities = listOf(sarajevo, mostar),
                accommodations = listOf(firstStay, mostarStay),
                days = listOf(
                    firstDay,
                    firstDay.copy(
                        id = "day10",
                        date = DAY_TWO.toString(),
                        dayNumber = firstDay.dayNumber + 1,
                        cityIds = listOf(mostar.id),
                        baseCityId = mostar.id,
                        accommodationIds = listOf(mostarStay.id),
                    ),
                ),
            ),
            packaged.assets,
        )
    }
}
