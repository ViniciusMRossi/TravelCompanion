package com.travelcompanion.app.data.trip

import java.time.LocalDate

/**
 * Single source of truth for packaged travel content.
 *
 * Screens never parse trip.json themselves; they read [TripContent].
 */
interface TripRepository {
    suspend fun load(): TripContent
}

/**
 * The packaged trip plus the lookups screens actually need.
 *
 * Packaged content is immutable. Runtime/device state (selected participant,
 * completed items, playback position, ...) never lives here.
 */
class TripContent(
    val trip: TripPackage,
    val assets: AssetResolver,
) {
    private val citiesById = trip.cities.associateBy(City::id)
    private val attractionsById = trip.attractions.associateBy(Attraction::id)
    private val walksById = trip.walks.associateBy(Walk::id)
    private val storiesById = trip.stories.associateBy(Story::id)
    private val audioGuidesById = trip.audioGuides.associateBy(AudioGuide::id)
    private val accommodationsById = trip.accommodations.associateBy(Accommodation::id)
    private val transportsById = trip.transports.associateBy(Transport::id)
    private val documentsById = trip.documents.associateBy(TripDocument::id)
    private val planBsById = trip.planBs.associateBy(PlanB::id)
    private val daysById = trip.days.associateBy(TripDay::id)
    private val participantsById = trip.trip.participants.associateBy(Participant::id)

    val info: TripInfo get() = trip.trip
    val days: List<TripDay> get() = trip.days

    /**
     * Length of the trip, taken from its date window rather than from how many
     * days happen to be packaged: "Dia 9 de 20" must stay true while content
     * for the other days is still being produced.
     */
    val totalDays: Int by lazy {
        runCatching {
            java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.parse(info.startDate),
                LocalDate.parse(info.endDate),
            ).toInt() + 1
        }.getOrDefault(trip.days.size)
    }

    fun city(id: String?): City? = id?.let(citiesById::get)
    fun attraction(id: String?): Attraction? = id?.let(attractionsById::get)
    fun walk(id: String?): Walk? = id?.let(walksById::get)
    fun story(id: String?): Story? = id?.let(storiesById::get)
    fun audioGuide(id: String?): AudioGuide? = id?.let(audioGuidesById::get)
    fun accommodation(id: String?): Accommodation? = id?.let(accommodationsById::get)
    fun transport(id: String?): Transport? = id?.let(transportsById::get)
    fun document(id: String?): TripDocument? = id?.let(documentsById::get)
    fun planB(id: String?): PlanB? = id?.let(planBsById::get)
    fun day(id: String?): TripDay? = id?.let(daysById::get)
    fun participant(id: String?): Participant? = id?.let(participantsById::get)

    fun emergencyProfile(countryCode: String): EmergencyProfile? =
        trip.emergencyProfiles.firstOrNull { it.countryCode.equals(countryCode, ignoreCase = true) }

    /**
     * The day the traveller is living right now.
     *
     * Before the trip starts the first day is shown, after it ends the last
     * one: Today must always have something to render offline.
     */
    fun dayFor(date: LocalDate): TripDay? {
        if (trip.days.isEmpty()) return null
        val ordered = trip.days.sortedBy { it.date }
        ordered.firstOrNull { it.date == date.toString() }?.let { return it }
        if (date < LocalDate.parse(ordered.first().date)) return ordered.first()
        return ordered.last()
    }

    /** Critical items of the day, plus those carried by the day's entities. */
    fun criticalItemsFor(day: TripDay): List<CriticalItem> {
        val fromEntities = day.transportIds.mapNotNull(::transport).flatMap { it.criticalItems } +
            day.accommodationIds.mapNotNull(::accommodation).flatMap { it.criticalItems }
        return (day.criticalItems + fromEntities).distinctBy(CriticalItem::id)
    }

    /** The attraction a timeline item points at, when it points at one. */
    fun attractionFor(item: TimelineItem): Attraction? =
        if (item.kind == "attraction") attraction(item.refId) else null

    fun walkFor(item: TimelineItem): Walk? =
        if (item.kind == "walk") walk(item.refId) else null

    /** The walk that departs from this attraction, if the day schedules one. */
    fun walkStartingAt(attraction: Attraction, day: TripDay?): Pair<Walk, TimelineItem>? {
        val timeline = day?.timeline ?: return null
        return timeline.asSequence()
            .mapNotNull { item -> walkFor(item)?.let { it to item } }
            .firstOrNull { (walk, _) -> walk.cityId == attraction.cityId }
    }
}
