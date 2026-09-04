package com.travelcompanion.app.feature.together

import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.design.formatPlaybackTime
import com.travelcompanion.app.service.playback.PlaybackState
import com.travelcompanion.app.service.sync.GroupSessionState

/**
 * Screen 09 state.
 *
 * Derived from the packaged trip, the local player and the group — in that
 * order of authority. Nothing here is fetched: the story, its text and its
 * chapters all come from `trip.json`, and the group only contributes who is
 * listening and how they are doing.
 */
data class ListenTogetherUiState(
    val storyTitle: String,
    val heroAssetPath: String?,
    val heroCaption: String,
    val chapterLabel: String,
    val walkTitle: String,
    val progress: Float,
    val elapsed: String,
    val total: String,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val listeners: List<ListenerUi>,
    val isDegraded: Boolean,
    val showTranscript: Boolean,
    val transcript: String?,
)

/**
 * Builds screen 09 from what is actually playing.
 *
 * The guide being listened to is the subject, not the walk's next stop: those
 * are different things, and showing one titled as the other is how the screen
 * ends up describing a story nobody is hearing. A story owns the guide only
 * when the package says so, and only then is there a transcript.
 */
fun buildListenTogetherState(
    content: TripContent,
    playback: PlaybackState,
    group: GroupSessionState,
    localParticipantId: String?,
    showTranscript: Boolean,
): ListenTogetherUiState? {
    val mediaId = playback.mediaId ?: return null
    val guide = content.audioGuide(mediaId)
    val story = content.trip.stories.firstOrNull { it.audioGuideId == mediaId }
    val walk = content.walk(story?.walkId) ?: content.trip.walks.firstOrNull()

    return ListenTogetherUiState(
        storyTitle = story?.title ?: guide?.title.orEmpty(),
        // No image: schema 1.1 gives a story no hero asset and no link to the
        // attraction that has one, so the hero is the striped placeholder with
        // the story's own name. Resolving it by guessing that `story.x` owns
        // `img.x.hero` would be a naming convention the package never promised
        // (open question, see IMPLEMENTATION-STATUS).
        heroAssetPath = null,
        heroCaption = "foto — ${story?.title ?: guide?.title.orEmpty()}",
        chapterLabel = playback.currentChapter
            ?.label((playback.currentChapterIndex ?: 0) + 1)
            ?: guide?.title.orEmpty(),
        walkTitle = walk?.title.orEmpty(),
        progress = playback.progress,
        elapsed = formatPlaybackTime(playback.positionMs),
        total = formatPlaybackTime(playback.durationMs),
        isPlaying = playback.isPlaying,
        isBuffering = playback.status == PlaybackState.Status.Buffering,
        listeners = listeners(content, group, localParticipantId),
        isDegraded = group.isDegraded,
        showTranscript = showTranscript,
        // The story's own body, and only when a story owns this guide. It is
        // deliberately not a second copy of the text: there is one place trip
        // prose lives, and a guide with no story behind it has none.
        transcript = story?.body,
    )
}

/**
 * Who is listening, named by the trip rather than by the group.
 *
 * The group knows ids; the names and initials come from `trip.participants`,
 * so a participant the group has not reported yet still has a name.
 */
private fun listeners(
    content: TripContent,
    group: GroupSessionState,
    localParticipantId: String?,
): List<ListenerUi> = group.participants.mapNotNull { participant ->
    val person = content.participant(participant.id) ?: return@mapNotNull null
    ListenerUi(
        initial = person.initial,
        name = if (person.id == localParticipantId) "${person.name} · você" else person.name,
        sync = participant.sync,
    )
}.ifEmpty {
    // Before the group has said anything, this phone still knows itself.
    val person = content.participant(localParticipantId) ?: return@ifEmpty emptyList()
    listOf(
        ListenerUi(
            initial = person.initial,
            name = "${person.name} · você",
            sync = ParticipantSync.Synchronized,
        ),
    )
}
