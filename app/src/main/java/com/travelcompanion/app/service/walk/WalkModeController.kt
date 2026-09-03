package com.travelcompanion.app.service.walk

import com.travelcompanion.app.data.trip.Story
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.walk.StoryTriggerStore
import com.travelcompanion.app.domain.walk.DeviceLocation
import com.travelcompanion.app.domain.walk.LocationQuality
import com.travelcompanion.app.domain.walk.StoryTriggerDecision
import com.travelcompanion.app.domain.walk.StoryTriggerRecord
import com.travelcompanion.app.domain.walk.WalkModeState
import com.travelcompanion.app.domain.walk.WalkPhase
import com.travelcompanion.app.domain.walk.arriveAt
import com.travelcompanion.app.domain.walk.completeWalking
import com.travelcompanion.app.domain.walk.decideStoryTrigger
import com.travelcompanion.app.domain.walk.finishWalking
import com.travelcompanion.app.domain.walk.pauseWalking
import com.travelcompanion.app.domain.walk.prepareWalk
import com.travelcompanion.app.domain.walk.startWalking
import com.travelcompanion.app.domain.walk.withAutomaticStories
import com.travelcompanion.app.domain.walk.withLocationQuality
import com.travelcompanion.app.service.location.LocationSource
import com.travelcompanion.app.service.playback.AudioGuideRequest
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.audioGuideRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the app does about Walk Mode, on the application rather than a screen.
 *
 * The phone is in a pocket for most of a walk, so nothing here may depend on
 * screen 07 being composed. The decisions live in `domain/walk` as pure
 * functions; this drives them with real positions and turns their answers into
 * audio, notifications and persisted records.
 *
 * Nothing here touches the network: a walk runs identically with the radios
 * off, and a story that fails to fire never ends the walk.
 */
class WalkModeController(
    private val locationSource: LocationSource,
    private val triggerStore: StoryTriggerStore,
    private val playbackController: PlaybackController,
    private val presence: WalkPresence,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(WalkModeState())
    val state: StateFlow<WalkModeState> = _state.asStateFlow()

    private var content: TripContent? = null
    private var locationJob: Job? = null
    private var triggered: Map<String, StoryTriggerRecord> = emptyMap()

    /** Screen 06: load the walk without starting anything or touching the GPS. */
    fun prepare(content: TripContent, walkId: String) {
        val walk = content.walk(walkId) ?: return
        this.content = content
        _state.value = prepareWalk(walk, content::story).withLocationQuality(currentQuality())
        scope.launch { triggered = triggerStore.all() }
    }

    fun setAutomaticStories(enabled: Boolean) {
        _state.update { it.withAutomaticStories(enabled) }
    }

    /** Re-reads permission after the traveller answers the system dialog. */
    fun refreshLocationQuality() {
        _state.update { it.withLocationQuality(currentQuality()) }
    }

    /** Screen 06's "Começar passeio", and resuming from Paused. */
    fun start() {
        val started = _state.value.startWalking()
        if (started.phase != WalkPhase.Active) return
        _state.value = started.withLocationQuality(currentQuality())
        // The persistent notification exists to satisfy Android's rules for
        // holding location in the background, and a foreground service of type
        // `location` is refused outright without a location permission. With no
        // location there is nothing to hold, so the walk simply runs without
        // it: refusing the permission costs automatic stories, never the walk.
        if (locationSource.hasPermission()) {
            presence.startWalk(started.walkTitle.orEmpty(), progressText(started))
        }
        observeLocation()
    }

    fun pause() {
        val paused = _state.value.pauseWalking()
        if (paused.phase != WalkPhase.Paused) return
        _state.value = paused
        stopObservingLocation()
        presence.updateWalk(paused.walkTitle.orEmpty(), progressText(paused))
    }

    /**
     * Screen 07's close button. Ends the walk and releases the GPS; audio the
     * walk started is left playing, because closing the walk mid-story is not
     * a request for silence.
     */
    fun finish() {
        val finishing = _state.value.finishWalking()
        if (finishing.phase != WalkPhase.Finishing) return
        stopObservingLocation()
        presence.stopWalk()
        _state.value = finishing.completeWalking()
    }

    /** Leaving screen 06 without starting: back to nothing running. */
    fun clear() {
        stopObservingLocation()
        presence.stopWalk()
        content = null
        _state.value = WalkModeState()
    }

    /**
     * Feeds one position through the decision and applies the answer.
     *
     * Public because it is the seam the decision is driven through: production
     * calls it from [observeLocation], and a device check can call it with a
     * known position instead of walking to Baščaršija.
     */
    fun onLocation(location: DeviceLocation) {
        if (_state.value.phase != WalkPhase.Active) return
        val trip = content ?: return

        _state.update { it.withLocationQuality(LocationQuality.Active) }

        val decision = decideStoryTrigger(
            location = location,
            stories = trip.trip.stories,
            triggered = triggered,
            walk = _state.value.routeContext(),
        )
        when (decision) {
            StoryTriggerDecision.Nothing -> Unit
            is StoryTriggerDecision.PlayInWalk -> arrive(decision.story, autoPlay = true)
            is StoryTriggerDecision.Notify -> arrive(decision.story, autoPlay = false)
        }
    }

    private fun arrive(story: Story, autoPlay: Boolean) {
        // Recorded before anything else can fail. An audio asset that is not
        // packaged must still count as arrived, or the same circle re-fires on
        // the very next fix.
        val at = now()
        triggered = triggered + (story.id to StoryTriggerRecord(story.id, triggeredAt = at))
        scope.launch { triggerStore.recordTriggered(story.id, at) }

        _state.update { it.arriveAt(story.id) }

        if (autoPlay) playStory(story, at) else presence.notifyStory(story.id, story.title, story.hook)

        val updated = _state.value
        if (updated.phase == WalkPhase.Active) {
            presence.updateWalk(updated.walkTitle.orEmpty(), progressText(updated))
        }
    }

    /**
     * Plays the story's audioguide when the package actually carries it.
     *
     * `audioGuideRequest` answers "not packaged" from content alone, and
     * [PlaybackController] ignores an unplayable guide rather than stopping
     * what is already playing (D021). A story with no audio at all is not an
     * error either: the walk continues either way.
     */
    private fun playStory(story: Story, at: Long) {
        val trip = content ?: return
        // Playable only: a guide that exists in the trip but is not packaged
        // comes back as NotPackaged, and recording playedAt for it would claim
        // audio that never reached the traveller. §20 keeps playedAt separate
        // from triggeredAt precisely so the two can disagree.
        val request = audioGuideRequest(trip, story.audioGuideId, subtitle = story.title)
        if (request !is AudioGuideRequest.Playable) return
        playbackController.playAudioGuide(request)
        scope.launch { triggerStore.recordPlayed(story.id, at) }
    }

    private fun observeLocation() {
        if (locationJob?.isActive == true) return
        if (!locationSource.hasPermission()) {
            // A refused permission costs automatic stories, not the walk.
            _state.update { it.withLocationQuality(LocationQuality.Denied) }
            return
        }
        _state.update { it.withLocationQuality(LocationQuality.Searching) }
        locationJob = scope.launch { locationSource.updates().collect(::onLocation) }
    }

    private fun stopObservingLocation() {
        locationJob?.cancel()
        locationJob = null
    }

    private fun currentQuality(): LocationQuality =
        if (locationSource.hasPermission()) LocationQuality.Searching else LocationQuality.Denied

    private fun progressText(state: WalkModeState): String {
        val total = state.stops.size
        if (total == 0) return "Passeio em andamento"
        return "Parada " + state.stopNumber + " de " + total
    }
}

/**
 * The Android side of running a walk: the persistent notification Android
 * requires while location runs in the foreground, and story notifications.
 *
 * Behind an interface so [WalkModeController] holds no Context and reads as
 * decisions rather than as service plumbing.
 */
interface WalkPresence {
    fun startWalk(title: String, text: String)
    fun updateWalk(title: String, text: String)
    fun stopWalk()
    fun notifyStory(storyId: String, title: String, hook: String)
}
