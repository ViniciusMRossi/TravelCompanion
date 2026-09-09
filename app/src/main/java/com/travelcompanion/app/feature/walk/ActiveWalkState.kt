package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.data.trip.Walk
import com.travelcompanion.app.design.formatPlaybackTime
import com.travelcompanion.app.domain.walk.LocationQuality
import com.travelcompanion.app.domain.walk.WalkModeState
import com.travelcompanion.app.service.playback.PlaybackState

/** Screen 07's location dot, in words rather than jargon. */
enum class WalkLocationUi { Active, Searching, Off }

/**
 * Screen 07 state.
 *
 * Derived, never stored: Walk Mode and playback each own their state, and this
 * is only how the two read as one screen.
 */
data class ActiveWalkUiState(
    val progressLabel: String,
    val remainingLabel: String,
    val totalStops: Int,
    val completedStops: Int,
    val nowPlayingEyebrow: String,
    val storyTitle: String,
    val storyContext: String?,
    val instruction: String?,
    val nextStopLabel: String?,
    val locationState: WalkLocationUi,
    val locationLabel: String,
    val audioProgress: Float,
    val elapsed: String,
    val total: String,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
)

fun buildActiveWalkState(
    walkState: WalkModeState,
    playback: PlaybackState,
    walk: Walk?,
): ActiveWalkUiState {
    val totalStops = walkState.stops.size
    val completed = walkState.visitedCount

    val locationState = when (walkState.locationQuality) {
        LocationQuality.Active -> WalkLocationUi.Active
        LocationQuality.Searching -> WalkLocationUi.Searching
        LocationQuality.Denied, LocationQuality.Unavailable, LocationQuality.Unknown -> WalkLocationUi.Off
    }

    // The story on screen is whatever the walk last arrived at — but only
    // while the player is actually on that story's guide.
    //
    // A story whose audio this build does not carry is never played (D021), so
    // arriving at it left the header announcing "TOCANDO AGORA · Latin Bridge"
    // over the guide that was still running from the stop before. Screen 07 is
    // read at a glance with the phone coming out of a pocket; it must not name
    // something that is not in the traveller's ear. When the two disagree the
    // walk names itself, which is the same thing this screen already does
    // before the first arrival — a blank hero on an ink field reads as a
    // broken screen (D053).
    val arrivedStop = walkState.currentStop
    val currentStop = arrivedStop?.takeIf { it.audioGuideId != null && it.audioGuideId == playback.mediaId }
    val storyTitle = currentStop?.title ?: walkState.walkTitle.orEmpty()
    val storyContext = when {
        currentStop == null -> walk?.subtitle ?: walkState.walkTitle
        totalStops > 0 -> "História ${walkState.stopNumber} de $totalStops · ${walkState.walkTitle}"
        else -> walkState.walkTitle
    }

    return ActiveWalkUiState(
        progressLabel = if (totalStops > 0) "${walkState.stopNumber} de $totalStops" else "Passeio",
        remainingLabel = remainingLabel(walk, totalStops, completed),
        totalStops = totalStops,
        completedStops = completed,
        // The same guard as `currentStop` above, and for the same reason: the
        // eyebrow announces the title under it, so it may only say "Tocando
        // agora" when the audio running is the one this screen is naming.
        // Over any other guide the walk names itself, and so does the eyebrow.
        nowPlayingEyebrow = if (playback.isPlaying && currentStop != null) {
            "Tocando agora"
        } else {
            "Passeio ativo"
        },
        storyTitle = storyTitle,
        storyContext = storyContext,
        // The instruction belongs to the stop just reached: it is what to do
        // next, so before the first arrival the walk's own route line stands
        // in. This one follows the arrival, not the audio — where to walk is
        // true whether or not the story could be narrated.
        instruction = arrivedStop?.instructionToNext ?: walk?.routeLabel,
        nextStopLabel = walkState.nextStop?.title,
        locationState = locationState,
        locationLabel = when (locationState) {
            WalkLocationUi.Active -> "Localização ativa"
            WalkLocationUi.Searching -> "Procurando localização"
            WalkLocationUi.Off -> "Sem localização · histórias não aparecem sozinhas"
        },
        audioProgress = playback.progress,
        elapsed = formatPlaybackTime(playback.positionMs),
        total = formatPlaybackTime(playback.durationMs),
        isPlaying = playback.isPlaying,
        isBuffering = playback.status == PlaybackState.Status.Buffering,
    )
}

/**
 * Rough time left, from the walk's own declared duration.
 *
 * Deliberately approximate and prefixed with "≈": the package knows how long
 * the walk takes, not how fast this traveller walks, and a precise-looking
 * number would be a promise the content cannot keep.
 */
private fun remainingLabel(walk: Walk?, totalStops: Int, completed: Int): String {
    val duration = walk?.durationMinutes ?: return ""
    if (totalStops <= 0) return "≈ $duration min"
    val remaining = duration * (totalStops - completed) / totalStops
    return "≈ $remaining min restantes"
}
