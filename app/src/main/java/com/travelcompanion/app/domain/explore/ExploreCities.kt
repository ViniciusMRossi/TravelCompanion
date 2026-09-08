package com.travelcompanion.app.domain.explore

import com.travelcompanion.app.data.trip.City
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay

/** One city of the day, as the band above screen 04 names it. */
data class ExploreCityChip(
    val cityId: String,
    val name: String,
)

/**
 * Which cities screen 04 offers for a day, and which one it opens on.
 *
 * [chips] is empty when there is nothing to choose between — see
 * [exploreCitiesOf] for why a single chip is worse than none.
 */
data class ExploreCities(
    val chips: List<ExploreCityChip>,
    val openAtCityId: String?,
) {
    companion object {
        val None = ExploreCities(chips = emptyList(), openAtCityId = null)
    }
}

/** A city is worth offering only if it has something to read. */
private fun City.hasContent(): Boolean =
    attractionIds.isNotEmpty() || storyIds.isNotEmpty() || walkIds.isNotEmpty() || menu != null

/**
 * Screen 04 resolves its city from the day's **timeline**, and deliberately
 * not the way screens 02, 03, 17 and 20 resolve theirs.
 *
 * `cityOfDay` answers *where the traveller sleeps* — the base — and that is
 * the right answer for the emergency country, the stay and the menu. It is the
 * wrong answer for Explorar, and not only on one day: day 17's base is Čilipi,
 * which packages no attraction, while the whole day happens in Dubrovnik and
 * its thirteen. Twelve of the twenty days carry more than one city with
 * content, and this screen used to show one of them.
 *
 * `cityIds` cannot decide it either: it includes the city the day *left* in
 * the morning, so day 3 begins with `amsterdam`, already behind.
 *
 * **The timeline is the only place in the package that says where a day
 * happens.** `cityIds` says where it passed through, the base says where it
 * ends up; only an itinerary row pointing at an attraction or a walk says what
 * the day is actually for. So:
 *
 *  1. the city of the first timeline row that points at an attraction or a
 *     walk, if that city has content;
 *  2. the base, if it has content;
 *  3. the first city of the day that has content;
 *  4. the base, content or not — the floor, and what this screen did before.
 *
 * The floor is not hypothetical: day 1 is São Paulo, an airport and a night
 * flight, and no city of that day packages a single attraction. It opens on
 * the base with no band, exactly as it always did.
 *
 * **A band is drawn only from two chips up.** Eight days pass through one city
 * with content, and a row holding a single chip is a control that cannot be
 * used — noise above the hero, on the majority of days.
 *
 * Nothing here breaks a tie by counting attractions. The timeline already
 * decides, and a tie broken by quantity is a number that usually agrees with
 * the right answer, which is a coincidence and not a guard (D089).
 */
fun exploreCitiesOf(content: TripContent, day: TripDay?): ExploreCities {
    if (day == null) return ExploreCities.None

    val base = day.baseCityId
        // fallback: a day that declares no base still has to name somewhere,
        // and its first city is where it begins (D090). Explorar reads it only
        // as the floor; what it opens on is decided below.
        ?: day.cityIds.firstOrNull()
    val withContent = day.cityIds.mapNotNull(content::city).filter { it.hasContent() }

    // (1) The first itinerary row that points at somewhere to read about. It
    // is read off `refId` rather than `kind` so a row typed as anything else
    // still counts if it names a real attraction or walk; a transport row
    // names a leg, resolves to neither, and decides nothing.
    val fromTimeline = day.timeline
        .asSequence()
        .mapNotNull { it.refId }
        .mapNotNull { ref -> content.attraction(ref)?.cityId ?: content.walk(ref)?.cityId }
        .mapNotNull(content::city)
        .firstOrNull { it.hasContent() }

    val openAt = fromTimeline?.id
        // (2) the base, when it has something to show;
        ?: base?.takeIf { content.city(it)?.hasContent() == true }
        // (3) the first city of the day that has anything;
        ?: withContent.firstOrNull()?.id
        // (4) and the base regardless, which is the floor.
        ?: base

    return ExploreCities(
        chips = if (withContent.size >= 2) {
            withContent.map { ExploreCityChip(cityId = it.id, name = it.name) }
        } else {
            emptyList()
        },
        openAtCityId = openAt,
    )
}
