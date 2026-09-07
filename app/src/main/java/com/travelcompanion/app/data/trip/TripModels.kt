package com.travelcompanion.app.data.trip

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed projection of trip-package/schema/trip.schema.json.
 *
 * JSON decoding ignores unknown keys, so the packaged content may carry
 * fields this projection does not model yet without breaking the app.
 */
@Serializable
data class TripPackage(
    val schemaVersion: String,
    val metadata: PackageMetadata,
    val trip: TripInfo,
    val assets: List<Asset> = emptyList(),
    val cities: List<City> = emptyList(),
    val attractions: List<Attraction> = emptyList(),
    val walks: List<Walk> = emptyList(),
    val stories: List<Story> = emptyList(),
    val audioGuides: List<AudioGuide> = emptyList(),
    val accommodations: List<Accommodation> = emptyList(),
    val transports: List<Transport> = emptyList(),
    val documents: List<TripDocument> = emptyList(),
    val planBs: List<PlanB> = emptyList(),
    val emergencyProfiles: List<EmergencyProfile> = emptyList(),
    val usefulApps: List<UsefulApp> = emptyList(),
    /**
     * City whose menu screen 20 borrows for a day whose own city has none.
     * Declared rather than discovered: taking the first city that happens to
     * carry a menu is the rule D097 exists to stop.
     */
    val fallbackMenuCityId: String? = null,
    val days: List<TripDay> = emptyList(),
)

@Serializable
data class PackageMetadata(
    val contentStatus: String,
    val isMockContent: Boolean = false,
    val generatedAt: String,
    val notes: String? = null,
)

@Serializable
data class TripInfo(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val startDate: String,
    val endDate: String,
    val locale: String,
    val defaultCountryCode: String,
    val coverAssetId: String? = null,
    val participants: List<Participant>,
    val sync: SyncConfig,
)

@Serializable
data class Participant(
    val id: String,
    val name: String,
    val subtitle: String? = null,
    val avatarAssetId: String? = null,
) {
    /** Avatar initial used by screen 01 and by participant pills. */
    val initial: String get() = name.take(1).uppercase()
}

@Serializable
data class SyncConfig(
    val enabled: Boolean,
    val groupId: String,
)

@Serializable
data class Asset(
    val id: String,
    val type: String,
    val path: String,
    val mimeType: String? = null,
    val description: String? = null,
    val sensitive: Boolean = false,
)

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class TripLocation(
    val name: String? = null,
    val address: String? = null,
    val geo: GeoPoint? = null,
    val mapsQuery: String? = null,
)

@Serializable
data class ActionLink(
    val label: String,
    val kind: String,
    val uri: String,
    val fallbackUri: String? = null,
    /** Schema default is true: assume an action needs the network unless the
     *  content package states otherwise, so offline UI never over-promises. */
    val requiresInternet: Boolean = true,
)

@Serializable
data class EditorialSection(
    val title: String,
    val body: String,
)

@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val description: String? = null,
    val distanceWalkMinutes: Int? = null,
    val priceNote: String? = null,
    val practicalNote: String? = null,
    val location: TripLocation? = null,
    val actions: List<ActionLink> = emptyList(),
)

@Serializable
data class City(
    val id: String,
    val name: String,
    val countryCode: String,
    val countryName: String,
    /** IANA zone the city's local times are written in (schema 1.1). */
    val timeZone: String,
    val heroAssetId: String? = null,
    val intro: String,
    val historySections: List<EditorialSection> = emptyList(),
    val audioGuideId: String? = null,
    val attractionIds: List<String> = emptyList(),
    val walkIds: List<String> = emptyList(),
    val storyIds: List<String> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(),
    val menu: Menu? = null,
)

/**
 * One city's food page (screen 20).
 *
 * [currency] and the owning city's country are what the screen labels the page
 * with, never the country of the day being viewed: a borrowed menu keeps its
 * own, or Bosnian food gets filed under someone else's cuisine (D070).
 */
@Serializable
data class Menu(
    val title: String,
    val intro: String,
    val currency: String,
    val pricesNote: String? = null,
    val meals: List<Meal> = emptyList(),
)

@Serializable
data class Meal(
    val name: String,
    val timeRange: String? = null,
    val dishes: List<Dish> = emptyList(),
)

@Serializable
data class Dish(
    val id: String,
    val name: String,
    val pronunciation: String? = null,
    val priceRange: String? = null,
    val photoAssetId: String? = null,
    /** What the photograph should show; survives even when no binary does. */
    val photoCaption: String? = null,
    val description: String,
    val history: String,
    val phrase: String,
    val phraseTranslation: String,
)

@Serializable
data class PracticalInfo(
    val openingHours: String? = null,
    val price: String? = null,
    val recommendedDurationMinutes: Int? = null,
    val reservationRequired: Boolean? = null,
    val accessibility: String? = null,
    val bestTime: String? = null,
    val requirements: List<String> = emptyList(),
)

@Serializable
data class Attraction(
    val id: String,
    val cityId: String,
    val name: String,
    val subtitle: String? = null,
    val heroAssetId: String? = null,
    val summary: String,
    val historySections: List<EditorialSection> = emptyList(),
    val interestingFacts: List<String> = emptyList(),
    val whatToObserve: List<String> = emptyList(),
    val location: TripLocation? = null,
    val practical: PracticalInfo? = null,
    val audioGuideId: String? = null,
    val documentIds: List<String> = emptyList(),
    val planBId: String? = null,
    val actions: List<ActionLink> = emptyList(),
)

@Serializable
data class AudioChapter(
    val title: String,
    val startSeconds: Double,
)

@Serializable
data class AudioGuide(
    val id: String,
    val title: String,
    val audioAssetId: String,
    val transcriptAssetId: String? = null,
    val durationSeconds: Int,
    val chapters: List<AudioChapter> = emptyList(),
) {
    val durationMinutes: Int get() = (durationSeconds + 59) / 60
}

@Serializable
data class LocationTrigger(
    val geo: GeoPoint,
    val radiusMeters: Double,
    /** Schema default is true: a story interrupts at most once per trip
     *  unless the content package opts into repeating it. */
    val notifyOncePerTrip: Boolean = true,
    val autoPlayInWalk: Boolean = false,
)

@Serializable
data class Story(
    val id: String,
    val cityId: String,
    val walkId: String? = null,
    val title: String,
    val hook: String,
    val body: String,
    val readDurationMinutes: Int? = null,
    val audioGuideId: String? = null,
    val trigger: LocationTrigger? = null,
    val priority: String = "normal",
)

@Serializable
data class WalkStop(
    val storyId: String,
    val order: Int,
    val instructionToNext: String? = null,
)

@Serializable
data class Walk(
    val id: String,
    val cityId: String,
    val title: String,
    val subtitle: String? = null,
    val heroAssetId: String? = null,
    val distanceMeters: Int,
    val durationMinutes: Int,
    val routeLabel: String? = null,
    val startLocation: TripLocation? = null,
    /** The attraction this walk departs from, when it departs from one. */
    val startAttractionId: String? = null,
    val stops: List<WalkStop> = emptyList(),
    val automaticStoriesDefault: Boolean = true,
    val sharedAudioSupported: Boolean = true,
)

@Serializable
data class TimeWindow(
    val from: String? = null,
    val until: String? = null,
)

/**
 * Criticality is an independent dimension over booking status.
 * [nominalTime] is the scheduled time; [actionByTime] is the deadline the
 * traveller actually has to meet. The approved design never merges them.
 */
@Serializable
data class CriticalItem(
    val id: String,
    val title: String,
    val nominalTime: String? = null,
    val actionByTime: String? = null,
    val instruction: String,
    val reason: String? = null,
    val actionLinks: List<ActionLink> = emptyList(),
) {
    /**
     * Combines two declarations of the same critical item.
     *
     * The same item is routinely declared twice — once curated on the day and
     * once in full on the transport or accommodation that owns it — and the
     * two copies rarely carry the same fields. Dropping either one silently
     * loses an action the traveller needs under time pressure, so every field
     * is kept: the first non-null wins and the action lists are unioned.
     */
    fun mergedWith(other: CriticalItem): CriticalItem {
        require(id == other.id) { "cannot merge critical items with different ids" }
        return copy(
            title = title.ifBlank { other.title },
            nominalTime = nominalTime ?: other.nominalTime,
            actionByTime = actionByTime ?: other.actionByTime,
            instruction = instruction.ifBlank { other.instruction },
            reason = reason ?: other.reason,
            actionLinks = (actionLinks + other.actionLinks).distinctBy { it.uri },
        )
    }
}

@Serializable
data class Accommodation(
    val id: String,
    val name: String,
    val cityId: String,
    val heroAssetId: String? = null,
    val location: TripLocation? = null,
    val checkIn: TimeWindow,
    val checkOut: TimeWindow,
    val bookingStatus: String? = null,
    val bookingReference: String? = null,
    val documentIds: List<String> = emptyList(),
    val contactName: String? = null,
    val contactPhone: String? = null,
    val instructions: String? = null,
    val criticalItems: List<CriticalItem> = emptyList(),
    val planBId: String? = null,
    val actions: List<ActionLink> = emptyList(),
)

@Serializable
data class TransportEndpoint(
    val name: String,
    val dateTime: String,
    /** IANA zone [dateTime] is written in; a leg can cross zones, so both ends declare one. */
    val timeZone: String,
    val location: TripLocation? = null,
    val platform: String? = null,
)

@Serializable
data class Transport(
    val id: String,
    val type: String,
    val operator: String? = null,
    val serviceNumber: String? = null,
    val origin: TransportEndpoint,
    val destination: TransportEndpoint,
    val bookingStatus: String? = null,
    val bookingReference: String? = null,
    val seat: String? = null,
    val price: String? = null,
    val documentIds: List<String> = emptyList(),
    val criticalItems: List<CriticalItem> = emptyList(),
    val planBId: String? = null,
    val actions: List<ActionLink> = emptyList(),
)

@Serializable
data class DocumentQr(
    val mode: String,
    val embeddedAssetId: String? = null,
    val text: String? = null,
)

@Serializable
@SerialName("document")
data class TripDocument(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String? = null,
    val assetId: String,
    val availableOffline: Boolean,
    val sensitive: Boolean = false,
    val participantIds: List<String> = emptyList(),
    val validFrom: String? = null,
    val validUntil: String? = null,
    val locator: String? = null,
    val qr: DocumentQr? = null,
)

@Serializable
data class PlanBStep(
    val title: String,
    val instruction: String,
    val deadline: String? = null,
    val expectedOutcome: String? = null,
    val actions: List<ActionLink> = emptyList(),
)

@Serializable
data class PlanB(
    val id: String,
    val scenario: String,
    val reassurance: String,
    val steps: List<PlanBStep> = emptyList(),
    val relatedDocumentIds: List<String> = emptyList(),
)

@Serializable
data class EmergencyContact(
    val label: String,
    val phone: String,
    val note: String? = null,
)

@Serializable
data class ShowToSomeone(
    val localLanguageText: String,
    val translation: String,
    val audioAssetId: String? = null,
)

@Serializable
data class EmergencyProfile(
    val id: String,
    val countryCode: String,
    val countryName: String,
    val generalEmergency: EmergencyContact,
    val police: EmergencyContact? = null,
    val ambulance: EmergencyContact? = null,
    val insurance: EmergencyContact? = null,
    val consular: EmergencyContact? = null,
    val showToSomeone: ShowToSomeone? = null,
)

@Serializable
data class UsefulApp(
    val id: String,
    val name: String,
    val countryCodes: List<String> = emptyList(),
    val dayIds: List<String> = emptyList(),
    val action: ActionLink,
)

@Serializable
data class WeatherFallback(
    val summary: String? = null,
    val minC: Double? = null,
    val maxC: Double? = null,
    val rainNote: String? = null,
    val windNote: String? = null,
)

@Serializable
data class Outfit(
    val wear: List<String> = emptyList(),
    val carry: List<String> = emptyList(),
    val special: List<String> = emptyList(),
)

@Serializable
data class TimelineItem(
    val id: String,
    val startTime: String,
    val endTime: String? = null,
    val kind: String,
    val title: String,
    /** Override only when the item does not happen in its day's zone; null means inherit. */
    val timeZone: String? = null,
    val detail: String? = null,
    val refId: String? = null,
    val bookingStatus: String? = null,
    val criticalItemIds: List<String> = emptyList(),
)

@Serializable
data class TripDay(
    val id: String,
    val date: String,
    val dayNumber: Int,
    /** IANA zone every timeline time on this day is written in, unless an item overrides it. */
    val timeZone: String,
    val title: String? = null,
    val cityIds: List<String> = emptyList(),
    val baseCityId: String? = null,
    val timeline: List<TimelineItem> = emptyList(),
    val criticalItems: List<CriticalItem> = emptyList(),
    val documentIds: List<String> = emptyList(),
    val transportIds: List<String> = emptyList(),
    val accommodationIds: List<String> = emptyList(),
    val planBId: String? = null,
    val weatherFallback: WeatherFallback? = null,
    val outfit: Outfit? = null,
    val usefulAppIds: List<String> = emptyList(),
)
