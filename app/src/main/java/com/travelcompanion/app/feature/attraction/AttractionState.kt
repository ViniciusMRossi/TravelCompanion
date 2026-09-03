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

data class AttractionUiState(
    val id: String,
    val name: String,
    val subtitle: String?,
    val cityLine: String,
    val heroAssetPath: String?,
    val heroCaption: String,
    val chips: List<String>,
    val summary: String,
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
