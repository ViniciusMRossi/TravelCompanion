package com.travelcompanion.app.service.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.travelcompanion.app.data.trip.LocationTrigger
import com.travelcompanion.app.data.trip.Story
import kotlin.math.max

/**
 * Which circles Android is asked to watch, and nothing else.
 *
 * **A geofence is an alarm clock, not a judge.** Nothing here compares a
 * distance with a radius or decides that a story should speak: the whole of
 * that rule is `decideStoryTrigger`, and a second copy of it is the defect
 * D096 recorded. What this registers is a *wake-up*, deliberately wider than
 * the content's own circle; what the wake-up costs, if it turns out to be
 * early, is one silent evaluation (D103).
 *
 * Behind an interface so [PassiveStoryDiscovery] can be driven on the JVM with
 * no Play Services, no permission and no device.
 */
interface StoryGeofences {

    /** True when Android would accept a background geofence right now. */
    fun canRegister(): Boolean

    /** Registers, or re-registers, one circle per story. Best-effort. */
    fun register(stories: List<Story>)

    /** Takes those circles down. Best-effort, and safe for ids never added. */
    fun cancel(storyIds: List<String>)
}

/**
 * The registration floor, and the one number in this file worth arguing about.
 *
 * The three real triggers declare `radiusMeters: 80`, and 80 m is what
 * `decideStoryTrigger` keeps applying — this constant never reaches the
 * decision. It exists because Android's geofencing is unreliable below about
 * 100 m: the platform promises no accuracy at that scale, latency grows as the
 * radius shrinks, and a circle that sometimes fails to fire is worse than no
 * circle at all. 120 m is the content's 80 m plus the ~40 m of error a fused
 * fix carries in a dense old town, which is the smallest ring that can be
 * trusted to ring.
 *
 * Registration-only, isolated and commented here for the same reason
 * `COORDINATE_CLUSTER_LIMIT_METERS` lives alone in the validator: a tuning
 * number that is not a rule must not be mistaken for one.
 */
const val GEOFENCE_FLOOR_RADIUS_METERS = 120f

/**
 * How long standing still inside the outer circle counts as loitering.
 *
 * ENTER alone fires once, at the rim — where a 120 m wake-up can easily be
 * outside the content's 80 m and the honest answer is silence. DWELL rings the
 * same alarm a second time for a traveller who stopped, without changing what
 * decides. One minute is a person reading a plaque, not a person walking past.
 */
private const val LOITERING_DELAY_MILLIS = 60_000

/**
 * The radius Android is asked to watch for one trigger.
 *
 * The floor or the content's own circle, whichever is wider — `max` and not a
 * replacement, because content is free to declare something larger than the
 * floor and 80 m is raised to a number Android can watch rather than being
 * rewritten. Pulled out of the builder so the one arithmetic step in this file
 * can be checked without Play Services, and so it is visible that nothing else
 * here does arithmetic on a radius at all.
 */
internal fun registrationRadiusMeters(trigger: LocationTrigger): Float =
    max(trigger.radiusMeters.toFloat(), GEOFENCE_FLOOR_RADIUS_METERS)

/**
 * Play Services geofencing, which is the one location mechanism this app is
 * allowed to use outside Walk Mode.
 *
 * The brief is literal that high-accuracy GPS must not run outside a walk, and
 * `LocationSource` holds that line. A geofence is not a flow of our own: the
 * system watches the circles with whatever it is already spending on location
 * and wakes the app once. That is why this may exist outside a walk where
 * `LocationSource.updates()` may not.
 *
 * No new dependency: `com.google.android.gms.location` was already on the
 * classpath for the fused provider, and brings [GeofencingClient] with it.
 */
class PlayServicesStoryGeofences(context: Context) : StoryGeofences {

    private val appContext = context.applicationContext
    private val client: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(appContext)
    }

    override fun canRegister(): Boolean =
        hasForegroundLocation(appContext) && hasBackgroundLocation(appContext)

    @SuppressLint("MissingPermission") // guarded by canRegister() on the line above
    override fun register(stories: List<Story>) {
        val fences = stories.mapNotNull(::geofenceFor)
        if (fences.isEmpty() || !canRegister()) return

        val request = GeofencingRequest.Builder()
            // A traveller who is already standing at the Sebilj when the app
            // registers should hear about it now, not on the next arrival.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(fences)
            .build()

        // Best-effort throughout, like every other Android call this app makes
        // about a story: passive discovery is enrichment, and a registration
        // that Play Services declines must cost nothing else.
        runCatching { client.addGeofences(request, transitionIntent()) }
    }

    override fun cancel(storyIds: List<String>) {
        if (storyIds.isEmpty()) return
        runCatching { client.removeGeofences(storyIds) }
    }

    /** One circle, at [registrationRadiusMeters]. */
    private fun geofenceFor(story: Story): Geofence? {
        val trigger = story.trigger ?: return null
        return Geofence.Builder()
            .setRequestId(story.id)
            .setCircularRegion(
                trigger.geo.latitude,
                trigger.geo.longitude,
                registrationRadiusMeters(trigger),
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL,
            )
            .setLoiteringDelay(LOITERING_DELAY_MILLIS)
            .build()
    }

    /**
     * Where a transition is delivered.
     *
     * Mutable on purpose, and the one place in this app where that is right:
     * Play Services fills the transition and the triggering position into this
     * Intent, and an immutable one would arrive empty. The Intent names its
     * own component, so nothing outside the app can aim it anywhere.
     */
    private fun transitionIntent(): PendingIntent {
        val intent = Intent(appContext, StoryGeofenceReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(appContext, 0, intent, flags)
    }
}

/** The permission Walk Mode already asks for on screen 06. */
internal fun hasForegroundLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Whether Android would let a circle be watched while the app is not on screen.
 *
 * Split from the foreground grant because Android splits it: from Android 10
 * this is a separate permission, asked separately; from Android 11 the system
 * shows no dialog for it at all and the only path is its own settings screen.
 * Before Android 10 there was no such thing, and the foreground grant is the
 * whole answer.
 *
 * A refused answer costs the traveller automatic stories, never the walk and
 * never the audio — the posture `LocationSource` already states, held here for
 * the passive path too.
 */
internal fun hasBackgroundLocation(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        hasForegroundLocation(context)
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
