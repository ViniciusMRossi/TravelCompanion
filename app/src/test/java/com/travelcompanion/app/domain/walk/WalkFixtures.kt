package com.travelcompanion.app.domain.walk

import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.data.trip.LocationTrigger
import com.travelcompanion.app.data.trip.Story
import com.travelcompanion.app.data.trip.Walk
import com.travelcompanion.app.data.trip.WalkStop

/** The packaged Sarajevo walk's real coordinates, so the fixtures stay honest. */
val MEETING_POINT = GeoPoint(43.8590, 18.4257)
val BRIDGE_POINT = GeoPoint(43.8578, 18.4289)

/** Far enough from both to be outside every radius in these tests. */
val AWAY_POINT = GeoPoint(43.8700, 18.4500)

fun story(
    id: String,
    point: GeoPoint,
    radiusMeters: Double = 120.0,
    notifyOncePerTrip: Boolean = true,
    autoPlayInWalk: Boolean = true,
    walkId: String? = "walk.sarajevo.historical",
): Story = Story(
    id = id,
    cityId = "sarajevo",
    walkId = walkId,
    title = "História $id",
    hook = "hook",
    body = "body",
    trigger = LocationTrigger(
        geo = point,
        radiusMeters = radiusMeters,
        notifyOncePerTrip = notifyOncePerTrip,
        autoPlayInWalk = autoPlayInWalk,
    ),
)

fun walk(vararg storyIds: String): Walk = Walk(
    id = "walk.sarajevo.historical",
    cityId = "sarajevo",
    title = "Caminhada Histórica de Sarajevo",
    distanceMeters = 1800,
    durationMinutes = 45,
    stops = storyIds.mapIndexed { i, id -> WalkStop(storyId = id, order = i + 1, instructionToNext = "siga") },
)

fun at(point: GeoPoint, accuracyMeters: Float? = 10f) = DeviceLocation(point, accuracyMeters)
