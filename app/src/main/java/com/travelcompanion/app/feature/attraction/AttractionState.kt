package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.Attraction
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import java.net.URLEncoder
import java.time.LocalDate

/** The operational strip on screen 05: when the walk leaves, and from where. */
data class WalkDepartureUi(
    val walkId: String,
    val time: String,
    val title: String,
    val meetingPoint: String?,
    val storyCount: Int,
)

/**
 * One titled stretch of narration on screen 05.
 *
 * [paragraphs] and not one string: `editorialSection.body` is a single field in
 * the schema, and written narration carries several paragraphs inside it. The
 * split happens here rather than in the composable so that "three paragraphs
 * stay three" is a fact a unit test can hold (D133).
 */
data class EditorialSectionUi(
    val title: String,
    val paragraphs: List<String>,
)

data class AttractionUiState(
    val id: String,
    val name: String,
    val subtitle: String?,
    val cityLine: String,
    val heroAssetPath: String?,
    val heroCaption: String,
    val chips: List<String>,
    val summary: String,
    /** Long-form narration, between the summary and the operational strip. */
    val historySections: List<EditorialSectionUi>,
    val departure: WalkDepartureUi?,
    val audioGuideId: String?,
    val audioLabel: String?,
    val audioAvailableOffline: Boolean,
    val mapsAction: ActionLink?,
    val directions: ActionLink,
    val whatToObserve: List<String>,
    val planBTitle: String?,
    val planBBody: String?,
)

/**
 * Screen 05 state.
 *
 * Editorial and operational material are separated here rather than in the
 * composable: the summary paragraph never carries a time, and the departure
 * strip never carries prose.
 */
fun buildAttractionState(
    content: TripContent,
    attractionId: String,
    date: LocalDate,
): AttractionUiState? {
    val attraction = content.attraction(attractionId) ?: return null
    val city = content.city(attraction.cityId)
    val day: TripDay? = content.dayFor(date)
    val audioGuide = content.audioGuide(attraction.audioGuideId)
    val audioOffline = audioGuide != null && content.assets.isAvailableOffline(audioGuide.audioAssetId)
    val planB = content.planB(attraction.planBId)

    val departure = content.walkStartingAt(attraction, day)?.let { (walk, item) ->
        WalkDepartureUi(
            walkId = walk.id,
            time = item.startTime,
            title = "${walk.title} sai daqui",
            meetingPoint = walk.startLocation?.name,
            storyCount = walk.stops.size,
        )
    }

    return AttractionUiState(
        id = attraction.id,
        name = attraction.name,
        subtitle = attraction.subtitle,
        cityLine = listOfNotNull(city?.name, city?.countryName).joinToString(" · "),
        heroAssetPath = content.assets.packagedPathIfPresent(attraction.heroAssetId),
        heroCaption = heroCaption(content, attraction),
        chips = buildList {
            audioGuide?.let { add("Audioguia ${it.durationMinutes} min") }
            if (audioOffline) add("Salvo offline")
            attraction.practical?.price?.let(::add)
            attraction.practical?.openingHours?.let(::add)
            attraction.practical?.recommendedDurationMinutes?.let { add("Visita ~$it min") }
        },
        summary = attraction.summary,
        historySections = attraction.historySections.map { section ->
            EditorialSectionUi(section.title, paragraphsOf(section.body))
        },
        departure = departure,
        audioGuideId = audioGuide?.id,
        audioLabel = audioGuide?.let { "Ouvir audioguia · ${it.durationMinutes} min" },
        audioAvailableOffline = audioOffline,
        mapsAction = attraction.actions.firstOrNull { it.kind == "maps" },
        directions = directionsAction(attraction),
        whatToObserve = attraction.whatToObserve,
        planBTitle = planB?.scenario,
        planBBody = planB?.reassurance,
    )
}

/**
 * A section's body, broken where its author broke it.
 *
 * The schema keeps the body in one string and the written content separates
 * paragraphs with a blank line. Handing that whole string to a single `Text`
 * is three hundred words in one slab, so it is split on the blank line and
 * each piece is drawn on its own. Blank runs of any length count as one break,
 * and `
` breaks exactly as `
` does.
 */
private fun paragraphsOf(body: String): List<String> =
    body.split(Regex("(\r?\n){2,}"))
        .map(String::trim)
        .filter(String::isNotEmpty)

/**
 * "Como chegar" is walking directions, not the same thing as opening the pin.
 * Try the navigation app first, then the web map — never a dead end.
 */
private fun directionsAction(attraction: Attraction): ActionLink {
    val geo = attraction.location?.geo
    val query = attraction.location?.mapsQuery ?: attraction.name
    val destination = geo?.let { "${it.latitude},${it.longitude}" } ?: query
    return ActionLink(
        label = "Como chegar",
        kind = "maps",
        uri = "google.navigation:q=${encode(destination)}&mode=w",
        fallbackUri = "https://www.google.com/maps/dir/?api=1&destination=${encode(destination)}",
        requiresInternet = false,
    )
}

private fun encode(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

private fun heroCaption(content: TripContent, attraction: Attraction): String {
    val description = content.assets.asset(attraction.heroAssetId)?.description
    return "foto — ${description ?: attraction.name}"
}
