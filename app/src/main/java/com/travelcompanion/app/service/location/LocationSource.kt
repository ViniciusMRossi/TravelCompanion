package com.travelcompanion.app.service.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.domain.walk.DeviceLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

/**
 * Where positions come from, behind an interface the decision layer never sees.
 *
 * Walk Mode's real risk is in what it decides, not in where the coordinates
 * came from, so the decision is tested against synthetic positions and this is
 * the only part that needs a device.
 */
interface LocationSource {

    /** True when the app may ask for positions at all. */
    fun hasPermission(): Boolean

    /**
     * Positions while collected, at Walk Mode's foreground cadence.
     *
     * Emits nothing at all — rather than failing — when permission is missing:
     * a refused permission costs the traveller automatic stories, never the
     * walk and never the audio.
     */
    fun updates(): Flow<DeviceLocation>
}

/**
 * Fused location, requested only while a walk is running.
 *
 * The brief is explicit that high-accuracy GPS must not run outside Walk Mode,
 * so this is a cold flow: it starts requesting when Walk Mode collects it and
 * stops the moment collection ends.
 */
class FusedLocationSource(
    context: Context,
    private val intervalMillis: Long = WALK_INTERVAL_MILLIS,
) : LocationSource {

    private val appContext = context.applicationContext
    private val client by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasPermission() immediately below
    override fun updates(): Flow<DeviceLocation> {
        if (!hasPermission()) return flow { }

        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(intervalMillis / 2)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    trySend(
                        DeviceLocation(
                            point = GeoPoint(location.latitude, location.longitude),
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        ),
                    )
                }
            }

            client.requestLocationUpdates(request, callback, appContext.mainLooper)
            awaitClose { client.removeLocationUpdates(callback) }
        }
    }

    private companion object {
        /** Responsive enough to notice arriving at a stop, cheap enough to walk with. */
        const val WALK_INTERVAL_MILLIS = 5_000L
    }
}
