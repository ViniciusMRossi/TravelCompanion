package com.travelcompanion.app.service.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.travelcompanion.app.appContainer
import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.domain.walk.DeviceLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Where a geofence transition lands, and the whole of the Android side of it.
 *
 * Deliberately thin: it turns a Play Services Intent into a position and hands
 * it to [PassiveStoryDiscovery], which is where the reading of the record, the
 * decision and the notification live. An error, an unexpected transition or a
 * wake-up with no position is silence — a story that cannot fire is never an
 * error, because location stories are enrichment.
 */
class StoryGeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition !in WAKING_TRANSITIONS) return

        // The position Play Services already had when it woke us. Used as it
        // arrives rather than asking for a fresh fix: the brief forbids
        // high-accuracy GPS outside Walk Mode, and `accuracyMeters` is carried
        // through so a wide fix decides nothing (`DeviceLocation.isUsableFor`).
        val fix = event.triggeringLocation ?: return
        val location = DeviceLocation(
            point = GeoPoint(fix.latitude, fix.longitude),
            accuracyMeters = if (fix.hasAccuracy()) fix.accuracy else null,
        )

        val discovery = context.appContainer.passiveStoryDiscovery
        // Reading DataStore and posting one notification is short, but it must
        // not happen on the receiver's thread — the same shape the boot
        // receiver uses for the deadlines.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                discovery.onEntered(location)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        /**
         * Arriving, and stopping once inside.
         *
         * DWELL is here because the registered circle is wider than the
         * content's (see `GEOFENCE_FLOOR_RADIUS_METERS`): a traveller can
         * cross the outer rim, decide nothing, and then stand at the plaque
         * without ever producing a second ENTER. It rings the same alarm
         * again; what decides is unchanged.
         */
        val WAKING_TRANSITIONS = setOf(
            Geofence.GEOFENCE_TRANSITION_ENTER,
            Geofence.GEOFENCE_TRANSITION_DWELL,
        )
    }
}
