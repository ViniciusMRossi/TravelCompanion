package com.travelcompanion.app.domain.walk

import com.travelcompanion.app.data.trip.Story

/**
 * What the app has already done about a story, per the brief's §20 runtime
 * record. Persisted, because `notifyOncePerTrip` means once per trip, not once
 * per process.
 */
data class StoryTriggerRecord(
    val storyId: String,
    val triggeredAt: Long,
    val playedAt: Long? = null,
    val dismissedAt: Long? = null,
)

/** The route context of the walk that is running, if one is. */
data class WalkRouteContext(
    val walkId: String,
    /** Stop story IDs in walk order. */
    val routeStoryIds: List<String>,
    /** The stop the traveller is walking towards, or null once the last one is done. */
    val nextStoryId: String?,
    /** The walk's automatic-stories preference, as the traveller left it on screen 06. */
    val automaticStories: Boolean,
)

/** What the app should do about the position it just received. */
sealed interface StoryTriggerDecision {

    /** Nothing in range, or nothing that has not already had its turn. */
    data object Nothing : StoryTriggerDecision

    /** In an active walk, with the story asking to be played automatically. */
    data class PlayInWalk(val story: Story) : StoryTriggerDecision

    /** Everything else in range: a notification the traveller can ignore. */
    data class Notify(val story: Story) : StoryTriggerDecision
}

/**
 * Decides what a position means, with no player, no notification and no GPS.
 *
 * The rules are the brief's §20, in the order they resolve:
 *
 * 1. `notifyOncePerTrip` is enforced against [triggered]: a story that already
 *    had its turn is never eligible again. This is what makes the decision
 *    idempotent — feeding the same position twice cannot fire twice.
 * 2. A fix too coarse for the trigger's own radius decides nothing yet.
 * 3. During an active walk the route wins: only stories on the route are
 *    eligible, and the stop the traveller is walking towards is preferred over
 *    any other circle they happen to be standing in. That is the route context
 *    overriding passive geofence behaviour.
 * 4. Outside a walk, the nearest eligible story wins, and the answer is always
 *    a notification — nothing auto-plays at someone who is not on a walk.
 *
 * A story that cannot fire is never an error: location stories are enrichment,
 * so every unmatched case simply returns [StoryTriggerDecision.Nothing].
 */
fun decideStoryTrigger(
    location: DeviceLocation,
    stories: List<Story>,
    triggered: Map<String, StoryTriggerRecord>,
    walk: WalkRouteContext? = null,
): StoryTriggerDecision {
    val inRange = stories.filter { story ->
        val trigger = story.trigger ?: return@filter false
        if (trigger.notifyOncePerTrip && triggered.containsKey(story.id)) return@filter false
        if (!location.isUsableFor(trigger.radiusMeters)) return@filter false
        distanceMeters(location.point, trigger.geo) <= trigger.radiusMeters
    }
    if (inRange.isEmpty()) return StoryTriggerDecision.Nothing

    if (walk == null) {
        val nearest = inRange.minByOrNull { distanceMeters(location.point, it.trigger!!.geo) }
        return nearest?.let(StoryTriggerDecision::Notify) ?: StoryTriggerDecision.Nothing
    }

    // Route context: off-route circles are ignored entirely while walking, so
    // a story belonging to another walk cannot interrupt this one.
    val onRoute = inRange.filter { it.id in walk.routeStoryIds }
    if (onRoute.isEmpty()) return StoryTriggerDecision.Nothing

    val chosen = onRoute.firstOrNull { it.id == walk.nextStoryId }
        ?: onRoute.minByOrNull { walk.routeStoryIds.indexOf(it.id) }
        ?: return StoryTriggerDecision.Nothing

    val autoPlay = walk.automaticStories && chosen.trigger?.autoPlayInWalk == true
    return if (autoPlay) StoryTriggerDecision.PlayInWalk(chosen) else StoryTriggerDecision.Notify(chosen)
}
