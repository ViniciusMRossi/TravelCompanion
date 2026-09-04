package com.travelcompanion.app.feature.together

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.feature.placeholder.PlaceholderScreen
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.sync.GroupSessionController
import com.travelcompanion.app.service.walk.WalkModeController
import kotlinx.coroutines.delay

/**
 * Screens 08 and 09 as one route.
 *
 * Screen 08 is a three-second transition into screen 09, not a place anyone
 * navigates to, so the countdown decides which is on screen rather than the
 * back stack — the same shape screens 06 and 07 use.
 */
@Composable
fun TogetherRoute(
    content: TripContent,
    participantId: String,
    playbackController: PlaybackController,
    walkModeController: WalkModeController,
    groupSessionController: GroupSessionController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val group by groupSessionController.state.collectAsStateWithLifecycle()
    val playback by playbackController.state.collectAsStateWithLifecycle()
    val walkState by walkModeController.state.collectAsStateWithLifecycle()

    // The subject of this screen is the guide that is playing, which is what
    // the group is being asked to listen to together.
    val mediaId = playback.mediaId
    val story = remember(mediaId) { content.trip.stories.firstOrNull { it.audioGuideId == mediaId } }

    var showTranscript by rememberSaveable(mediaId) { mutableStateOf(false) }

    DisposableEffect(participantId) {
        groupSessionController.join()
        // Leaving is deliberate and local-only: it stops following the group,
        // never the audio.
        onDispose { groupSessionController.leave() }
    }

    // Proposing the shared start once, when the traveller arrives here.
    LaunchedEffect(mediaId) {
        val id = mediaId ?: return@LaunchedEffect
        groupSessionController.startTogether(id, playback.positionMs)
    }

    // 3 → 2 → 1, one second apart, matching the anchor published to the group.
    LaunchedEffect(group.isStarting) {
        while (groupSessionController.state.value.countdown != null) {
            delay(1_000L)
            groupSessionController.tickCountdown()
        }
    }

    if (mediaId == null) {
        PlaceholderScreen(
            title = "Ouvir juntos",
            message = "Comece um audioguia para ouvir junto.",
            actionLabel = "Voltar",
            onAction = onBack,
        )
        return
    }

    if (group.isStarting) {
        SyncingScreen(
            storyTitle = story?.title ?: content.audioGuide(mediaId)?.title.orEmpty(),
            participants = content.info.participants.map { person ->
                SyncingParticipantUi(
                    initial = person.initial,
                    name = person.name,
                    label = "Sincronizado",
                )
            },
            countdown = group.countdown,
            modifier = modifier,
        )
        return
    }

    val state = buildListenTogetherState(
        content = content,
        playback = playback,
        group = group,
        localParticipantId = participantId,
        showTranscript = showTranscript,
    ) ?: return

    ListenTogetherScreen(
        state = state,
        onBack = onBack,
        onTogglePlayPause = playbackController::togglePlayPause,
        onSkipBack = { playbackController.seekBy(-SKIP_MILLIS) },
        onSkipForward = { playbackController.seekBy(SKIP_MILLIS) },
        // Offered only when a story actually backs this guide.
        onToggleTranscript = { showTranscript = !showTranscript }.takeIf { story != null },
        // Offered only while the walk still has somewhere to go.
        onNextStory = walkState.nextStop?.let { { onBack() } },
        modifier = modifier,
    )
}

/** ±15 on screen 09's transport, the same movement screen 07 uses (D025). */
private const val SKIP_MILLIS = 15_000L
