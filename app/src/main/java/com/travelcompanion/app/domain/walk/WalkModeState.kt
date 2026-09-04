package com.travelcompanion.app.domain.walk

import com.travelcompanion.app.data.trip.Story
import com.travelcompanion.app.data.trip.Walk

/** The brief's §19 states, kept explicit rather than inferred from flags. */
enum class WalkPhase { Idle, Preparing, Active, Paused, Finishing, Completed }

/**
 * How much the app can currently tell about where the traveller is.
 *
 * Screen 07 shows this as a dot and a word, never as jargon, and no value here
 * stops the walk: a walk with [Denied] location is a walk without automatic
 * stories, not a broken walk.
 */
enum class LocationQuality { Unknown, Denied, Unavailable, Searching, Active }

/** One stop of the walk, flattened for the screen and the state machine. */
data class WalkStopState(
    val storyId: String,
    val title: String,
    val instructionToNext: String?,
    val visited: Boolean = false,
    /**
     * The guide this story would play, or null when it has none.
     *
     * Carried so screen 07 can tell whether the story it arrived at is the one
     * the player is actually on. Whether that guide is *packaged* is content's
     * answer, not this state's (D021) — what the screen compares against is
     * the player itself (D053).
     */
    val audioGuideId: String? = null,
)

/**
 * Walk Mode as a value.
 *
 * Every transition below is a pure function so the machine can be tested with
 * synthetic positions and no GPS, which is where its real risk lives.
 */
data class WalkModeState(
    val phase: WalkPhase = WalkPhase.Idle,
    val walkId: String? = null,
    val walkTitle: String? = null,
    val stops: List<WalkStopState> = emptyList(),
    /** Index of the stop the traveller has arrived at, once they have. */
    val currentStopIndex: Int? = null,
    val locationQuality: LocationQuality = LocationQuality.Unknown,
    val automaticStories: Boolean = true,
) {
    val isRunning: Boolean get() = phase == WalkPhase.Active || phase == WalkPhase.Paused

    val currentStop: WalkStopState? get() = currentStopIndex?.let(stops::getOrNull)

    val previousStops: List<WalkStopState> get() = stops.filter { it.visited }

    /** The stop being walked towards: the first not yet visited. */
    val nextStop: WalkStopState? get() = stops.firstOrNull { !it.visited }

    val visitedCount: Int get() = stops.count { it.visited }

    /** "3 de 7" on screen 07 — one-based, and never past the end. */
    val stopNumber: Int get() = (currentStopIndex?.plus(1)) ?: minOf(visitedCount + 1, stops.size)

    /** The route context the trigger decision needs, or null when not walking. */
    fun routeContext(): WalkRouteContext? {
        val id = walkId ?: return null
        if (phase != WalkPhase.Active) return null
        return WalkRouteContext(
            walkId = id,
            routeStoryIds = stops.map(WalkStopState::storyId),
            nextStoryId = nextStop?.storyId,
            automaticStories = automaticStories,
        )
    }
}

/**
 * Loads a walk into [WalkPhase.Preparing] — screen 06, nothing running yet.
 *
 * Stops come from the walk in its declared order; a stop whose story is absent
 * from the package is dropped rather than rendered as a blank line.
 */
fun prepareWalk(walk: Walk, stories: (String) -> Story?): WalkModeState {
    val stops = walk.stops
        .sortedBy { it.order }
        .mapNotNull { stop ->
            val story = stories(stop.storyId) ?: return@mapNotNull null
            WalkStopState(
                storyId = stop.storyId,
                title = story.title,
                instructionToNext = stop.instructionToNext,
                audioGuideId = story.audioGuideId,
            )
        }
    return WalkModeState(
        phase = WalkPhase.Preparing,
        walkId = walk.id,
        walkTitle = walk.title,
        stops = stops,
        automaticStories = walk.automaticStoriesDefault,
    )
}

/**
 * The transitions, and only these. Anything not listed is ignored and returns
 * the state unchanged: an out-of-order signal — a stale location callback, a
 * double tap on "Começar passeio" — must not move the machine sideways.
 */
fun WalkModeState.startWalking(): WalkModeState =
    if (phase == WalkPhase.Preparing || phase == WalkPhase.Paused) copy(phase = WalkPhase.Active) else this

fun WalkModeState.pauseWalking(): WalkModeState =
    if (phase == WalkPhase.Active) copy(phase = WalkPhase.Paused) else this

fun WalkModeState.finishWalking(): WalkModeState =
    if (isRunning) copy(phase = WalkPhase.Finishing) else this

fun WalkModeState.completeWalking(): WalkModeState =
    if (phase == WalkPhase.Finishing) copy(phase = WalkPhase.Completed, currentStopIndex = null) else this

fun WalkModeState.withLocationQuality(quality: LocationQuality): WalkModeState =
    copy(locationQuality = quality)

fun WalkModeState.withAutomaticStories(enabled: Boolean): WalkModeState =
    copy(automaticStories = enabled)

/**
 * Records arrival at a stop.
 *
 * Marking is idempotent and monotonic: arriving again at a stop already
 * visited changes nothing, so a repeated fix cannot rewind the walk or
 * re-announce a story.
 */
fun WalkModeState.arriveAt(storyId: String): WalkModeState {
    if (phase != WalkPhase.Active) return this
    val index = stops.indexOfFirst { it.storyId == storyId }
    if (index < 0) return this
    if (stops[index].visited) return this
    return copy(
        currentStopIndex = index,
        stops = stops.mapIndexed { i, stop -> if (i == index) stop.copy(visited = true) else stop },
    )
}
