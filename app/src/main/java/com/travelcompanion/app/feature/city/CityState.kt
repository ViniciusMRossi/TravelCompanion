package com.travelcompanion.app.feature.city

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.editorial.EditorialSectionUi
import com.travelcompanion.app.domain.editorial.editorialSectionsOf
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** One chapter of the city guide, numbered, with its own length. */
data class CityChapterUi(
    val index: Int,
    val title: String,
    val durationLabel: String,
)

/** The teal card at the top of the editorial layer. */
data class CityGuideUi(
    val id: String,
    val title: String,
    /** "3 capítulos · 34 min" */
    val summary: String,
    val isPackaged: Boolean,
    val chapters: List<CityChapterUi>,
)

data class CityAttractionUi(
    val id: String,
    val name: String,
    val subtitle: String?,
    val heroAssetPath: String?,
    val heroCaption: String,
    /** The hour it appears in the itinerary, when the trip schedules it. */
    val scheduleTime: String?,
    val pills: List<String>,
)

data class CityWalkUi(
    val id: String,
    val title: String,
    val subtitle: String?,
    val distanceLabel: String,
    val durationLabel: String,
    val storiesLabel: String,
)

data class CityRestaurantUi(
    val name: String,
    val description: String?,
    val distanceLabel: String?,
    val practicalNote: String?,
    val action: ActionLink?,
)

data class CityStoryUi(
    val id: String,
    val title: String,
    val hook: String,
    val body: String,
    val durationLabel: String?,
    val audioGuideId: String?,
)

/** Screen 04 state. */
data class CityUiState(
    val cityId: String,
    val name: String,
    val countryName: String,
    /** The stretch of the trip spent here, from the packaged days. */
    val stayLabel: String?,
    val heroAssetPath: String?,
    val heroCaption: String,
    val intro: String,
    /**
     * Long-form narration about the city, between the intro and the guide.
     *
     * The same `$defs/editorialSection` screen 05 has drawn since D133, and
     * built by the same `editorialSectionsOf`. No packaged city fills it today
     * — 0 of 19 — so it draws nothing, and drawing nothing is the point: an
     * empty list is no heading and no reserved space (D157).
     */
    val historySections: List<EditorialSectionUi>,
    val guide: CityGuideUi?,
    val attractions: List<CityAttractionUi>,
    val walk: CityWalkUi?,
    val restaurants: List<CityRestaurantUi>,
    val stories: List<CityStoryUi>,
)

/**
 * Builds screen 04 — the editorial layer of a city.
 *
 * Context, not operation: nothing here carries a departure, a platform or a
 * deadline. The one hour it does show is the hour an attraction appears in the
 * itinerary, which is what tells the reader where in the day this sits.
 *
 * The chapter list is where D025 finally lands: `seekToChapter` has been
 * implemented and tested since Phase 2 with no approved surface to drive it,
 * and this is that surface.
 */
fun buildCityState(
    content: TripContent,
    cityId: String?,
    today: LocalDate,
): CityUiState? {
    val city = content.city(cityId) ?: return null
    val locale = Locale.forLanguageTag("pt-BR")
    val guide = content.audioGuide(city.audioGuideId)

    // Where each attraction sits in the days that happen in this city.
    val scheduled = content.days
        .filter { city.id in it.cityIds }
        .flatMap { it.timeline }
        .filter { it.kind == "attraction" && it.refId != null }
        .associate { it.refId!! to it.startTime }

    return CityUiState(
        cityId = city.id,
        name = city.name,
        countryName = city.countryName,
        stayLabel = stayLabel(content, city.id, locale),
        heroAssetPath = content.assets.packagedPathIfPresent(city.heroAssetId),
        heroCaption = "foto — ${city.name}",
        intro = city.intro,
        historySections = editorialSectionsOf(city.historySections),
        guide = guide?.let {
            CityGuideUi(
                id = it.id,
                title = it.title,
                summary = "${it.chapters.size} capítulos · ${it.durationMinutes} min",
                isPackaged = content.assets.isAvailableOffline(it.audioAssetId),
                chapters = it.chapters
                    .sortedBy { chapter -> chapter.startSeconds }
                    .mapIndexed { index, chapter ->
                        CityChapterUi(
                            index = index,
                            title = chapter.title,
                            durationLabel = chapterLength(
                                startSeconds = chapter.startSeconds,
                                nextStartSeconds = it.chapters
                                    .sortedBy { c -> c.startSeconds }
                                    .getOrNull(index + 1)
                                    ?.startSeconds
                                    ?: it.durationSeconds.toDouble(),
                            ),
                        )
                    },
            )
        },
        attractions = city.attractionIds.mapNotNull(content::attraction).map { attraction ->
            val attractionGuide = content.audioGuide(attraction.audioGuideId)
            CityAttractionUi(
                id = attraction.id,
                name = attraction.name,
                subtitle = attraction.subtitle,
                heroAssetPath = content.assets.packagedPathIfPresent(attraction.heroAssetId),
                heroCaption = "foto — ${attraction.name}",
                scheduleTime = scheduled[attraction.id],
                pills = buildList {
                    if (attractionGuide != null) add("Audioguia")
                    // The badge is earned by a file in this build, never by a
                    // promise in the package (D013).
                    if (content.assets.isAvailableOffline(attractionGuide?.audioAssetId)) add("Offline")
                    // The price is not a pill and does not belong on a card in
                    // a carousel, which has less room than screen 05 and not
                    // more. It is one tap away, whole, in that screen's
                    // operational block (D154).
                },
            )
        },
        walk = city.walkIds.firstNotNullOfOrNull(content::walk)?.let { walk ->
            CityWalkUi(
                id = walk.id,
                title = walk.title,
                subtitle = walk.subtitle,
                distanceLabel = distanceLabel(walk.distanceMeters),
                durationLabel = "${walk.durationMinutes} min",
                storiesLabel = "${walk.stops.size} histórias",
            )
        },
        restaurants = city.restaurants.map { restaurant ->
            CityRestaurantUi(
                name = restaurant.name,
                description = restaurant.description,
                distanceLabel = restaurant.distanceWalkMinutes?.let { "$it min a pé" },
                practicalNote = listOfNotNull(restaurant.priceNote, restaurant.practicalNote)
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(" · "),
                action = restaurant.actions.firstOrNull { it.kind == "maps" },
            )
        },
        stories = city.storyIds.mapNotNull(content::story).map { story ->
            CityStoryUi(
                id = story.id,
                title = story.title,
                hook = story.hook,
                body = story.body,
                durationLabel = story.readDurationMinutes?.let { "$it min" },
                audioGuideId = story.audioGuideId,
            )
        },
    )
}

/** "21 de setembro", or "21 – 23 de setembro" when the trip stays longer. */
private fun stayLabel(content: TripContent, cityId: String, locale: Locale): String? {
    val dates = content.days
        .filter { cityId in it.cityIds }
        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        .sorted()
    val first = dates.firstOrNull() ?: return null
    val last = dates.last()

    fun month(date: LocalDate) = date.month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)

    return when {
        first == last -> "${first.dayOfMonth} de ${month(first)}"
        first.month == last.month -> "${first.dayOfMonth} – ${last.dayOfMonth} de ${month(first)}"
        else -> "${first.dayOfMonth} de ${month(first)} – ${last.dayOfMonth} de ${month(last)}"
    }
}

/** "1,8 km" or "800 m". */
private fun distanceLabel(meters: Int): String =
    if (meters >= 1_000) {
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", meters / 1_000.0)
    } else {
        "$meters m"
    }

/** How long a chapter runs, from where the next one starts. */
private fun chapterLength(startSeconds: Double, nextStartSeconds: Double): String {
    val seconds = (nextStartSeconds - startSeconds).coerceAtLeast(0.0)
    val minutes = Math.round(seconds / 60.0).toInt().coerceAtLeast(1)
    return "$minutes min"
}
