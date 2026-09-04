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
    /**
     * The amber note under the listeners, or null when there is nothing to
     * say. One note, because there is one group.
     */
    val groupNote: String?,
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
        groupNote = groupNote(group),
        showTranscript = showTranscript,
        // The story's own body, and only when a story owns this guide. It is
        // deliberately not a second copy of the text: there is one place trip
        // prose lives, and a guide with no story behind it has none.
        transcript = story?.body,
    )
}

/**
 * What the group's state is worth saying out loud, in one sentence.
 *
 * Divergence comes first because it is the more specific fact: a group that is
 * on another story is reachable, and saying it could not be synchronized would
 * send the traveller looking for a signal problem that is not there.
 */
private fun groupNote(group: GroupSessionState): String? = when {
    group.isDiverged -> "O grupo está ouvindo outra história. " +
        "Seu audioguia continua funcionando normalmente."
    group.isDegraded -> "Não foi possível sincronizar o grupo agora. " +
        "Seu audioguia continua funcionando normalmente."
    else -> null
}

/**
 * Who is listening, named by the trip rather than by the group.
 *
 * The group knows ids; the names and initials come from `trip.participants`,
 * so a participant the group has not reported yet still has a name.
 *
 * This phone listening to a different story is reported on this phone's own
 * row, and nowhere else: the group's payload carries one shared guide and no
 * per-participant one, so the other phone cannot be told (D042).
 */
private fun listeners(
    content: TripContent,
    group: GroupSessionState,
    localParticipantId: String?,
): List<ListenerUi> = group.participants.mapNotNull { participant ->
    val person = content.participant(participant.id) ?: return@mapNotNull null
    val isLocal = person.id == localParticipantId
    val diverged = isLocal && group.isDiverged
    ListenerUi(
        initial = person.initial,
        name = if (isLocal) "${person.name} · você" else person.name,
        // Not in step, so not the green dot — but nothing here is reconnecting
        // and the row must not say so. The state carries the dot; the label
        // carries what is actually true.
        sync = if (diverged) ParticipantSync.Reconnecting else participant.sync,
        label = if (diverged) OTHER_STORY else participant.sync.label(),
    )
}.ifEmpty {
    // Before the group has said anything, this phone still knows itself.
    val person = content.participant(localParticipantId) ?: return@ifEmpty emptyList()
    val diverged = group.isDiverged
    listOf(
        ListenerUi(
            initial = person.initial,
            name = "${person.name} · você",
            sync = if (diverged) ParticipantSync.Reconnecting else ParticipantSync.Synchronized,
            label = if (diverged) OTHER_STORY else ParticipantSync.Synchronized.label(),
        ),
    )
}

/** The approved words for each state, and nowhere else in the screen. */
private fun ParticipantSync.label(): String = when (this) {
    ParticipantSync.Synchronized -> "Sincronizado"
    ParticipantSync.Reconnecting -> "Sincronizando novamente"
}

/**
 * Written for a state the approved design does not draw: both phones on screen
 * 09 with different guides loaded. It keeps the register of the states that
 * are drawn — what is true, in the traveller's words, with no network in it.
 */
private const val OTHER_STORY = "Ouvindo outra história"
