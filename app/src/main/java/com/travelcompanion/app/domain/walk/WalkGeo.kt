package com.travelcompanion.app.domain.walk

import com.travelcompanion.app.data.trip.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Great-circle distance in metres.
 *
 * Written here rather than taken from `android.location.Location` so the
 * trigger decision stays a pure function that tests can drive with synthetic
 * positions, with no GPS and no Android framework in the way.
 */
fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(to.longitude - from.longitude)

    val h = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(h)))
}

/**
 * A position as the app consumes it, with no Android type attached.
 *
 * `accuracyMeters` is carried because a fix wide enough to cover half the old
 * town should not be allowed to claim an arrival: see [DeviceLocation.isUsableFor].
 */
data class DeviceLocation(
    val point: GeoPoint,
    val accuracyMeters: Float? = null,
) {
    /**
     * Whether this fix is precise enough to decide an arrival at a circle of
     * [radiusMeters].
     *
     * A fix whose own error is larger than the trigger radius cannot tell
     * "inside" from "somewhere nearby", and a story that fires from the wrong
     * block is worse than one that fires late — location stories are
     * enrichment, so the cost of waiting for a better fix is nearly zero.
     */
    fun isUsableFor(radiusMeters: Double): Boolean {
        val accuracy = accuracyMeters ?: return true
        return accuracy <= radiusMeters
    }
}
