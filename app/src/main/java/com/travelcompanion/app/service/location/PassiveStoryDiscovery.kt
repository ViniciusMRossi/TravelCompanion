package com.travelcompanion.app.service.location

import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.walk.StoryTriggerStore
import com.travelcompanion.app.domain.walk.DeviceLocation
import com.travelcompanion.app.domain.walk.StoryTriggerDecision
import com.travelcompanion.app.domain.walk.decideStoryTrigger

/**
 * Posting one story the traveller walked into, outside a walk.
 *
 * Behind an interface so this class holds no Context, exactly as
 * [com.travelcompanion.app.service.walk.WalkPresence] does for the active
 * path. The production implementation is the same class that posts the walk's
 * story notifications, so there is one permission guard and one builder rather
 * than a third way to notify.
 */
fun interface StoryDiscoveryPresence {
    fun notifyStoryOutsideWalk(storyId: String, title: String, hook: String)
}

/**
 * Passive story discovery: the circles Android watches while nobody is walking.
 *
 * The shape is the one the brief's Location section asks for — "geofence /
 * background-capable location mechanisms", "low power", "approximate trigger"
 * — and the shape D103 settled: **the geofence wakes the app, the pure
 * function decides.** [StoryGeofences] registers a wider circle than the
 * content declares because Android cannot be trusted with an 80 m one; the
 * position that arrives with the wake-up then goes through
 * [decideStoryTrigger] at the content's own radius. Nothing here compares a
 * distance with a radius.
 *
 * Free of Android on purpose: the receiver is four lines of plumbing, and
 * everything worth testing is here.
 */
class PassiveStoryDiscovery(
    private val trip: suspend () -> TripContent?,
    private val triggerStore: StoryTriggerStore,
    private val geofences: StoryGeofences,
    private val presence: StoryDiscoveryPresence,
    private val isWalkRunning: () -> Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Whether passive discovery can happen at all, for screen 19 to say so. */
    fun canDiscover(): Boolean = geofences.canRegister()

    /**
     * Brings the registered set in line with the trip and the record.
     *
     * Called when the app has content and is on screen, and after a restart —
     * geofences do not survive a reboot, and the boot receiver that puts the
     * deadlines back puts these back with them (D092, D103).
     *
     * Every story that carries a trigger and has not yet had its turn is
     * registered; the ones that have are taken down. With `notifyOncePerTrip`
     * true — the schema's default, and what all three real triggers get — a
     * circle that has already spoken can only cost battery.
     *
     * Note what this does *not* do: it does not ask whether the traveller is
     * near anything. Eligibility here is "has a trigger, has not fired"; where
     * the traveller is remains [decideStoryTrigger]'s question alone.
     */
    suspend fun refresh() {
        val content = trip() ?: return
        val triggered = triggerStore.all()

        val (eligible, spent) = content.trip.stories
            .filter { it.trigger != null }
            .partition { story ->
                !(story.trigger!!.notifyOncePerTrip && triggered.containsKey(story.id))
            }

        geofences.cancel(spent.map { it.id })
        // Without the permission there is nothing registered to bring in line:
        // Android drops the circles when it is revoked, and refuses them when
        // it was never granted. The traveller keeps the walk, the audio and
        // every story on the city screen; only this does not happen.
        if (!geofences.canRegister()) return
        geofences.register(eligible)
    }

    /**
     * One geofence transition, decided at the content's own radius.
     *
     * @param location the position Play Services delivered with the wake-up.
     */
    suspend fun onEntered(location: DeviceLocation) {
        // A walk owns the decision while it runs, and holds the record in
        // memory from its start — a write made behind its back would not be
        // seen by that copy, and the same story could speak twice. Passive
        // discovery is silent for as long as a walk is running (D105 (b)).
        if (isWalkRunning()) return

        val content = trip() ?: return
        val decision = decideStoryTrigger(
            location = location,
            stories = content.trip.stories,
            triggered = triggerStore.all(),
            // No walk: outside one, the answer is always a notification, and
            // nothing auto-plays at someone who is not walking.
            walk = null,
        )
        // Nothing is the normal answer, not an error: the wake-up circle is
        // wider than the content's, so an early transition is expected to
        // decide nothing at all.
        val story = (decision as? StoryTriggerDecision.Notify)?.story ?: return

        // Recorded before anything else can fail — this is what stops the
        // second notification for the same story, and a notification that is
        // refused must still count as the story's turn.
        triggerStore.recordTriggered(story.id, now())
        presence.notifyStoryOutsideWalk(story.id, story.title, story.hook)
        // It has spoken once, and `notifyOncePerTrip` means once. A circle
        // that can no longer say anything is battery and nothing else.
        geofences.cancel(listOf(story.id))
    }
}
