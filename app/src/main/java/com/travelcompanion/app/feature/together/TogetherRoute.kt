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
import com.travelcompanion.app.service.playback.audioGuideRequest
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
        // Arriving here is the traveller asking to listen with the group —
        // the prototype has one way in, the "Ouvir juntos" button under screen
        // 07's transport, and it is the same button on both phones. What the
        // ask means depends on what the group is already doing, so the
        // controller decides once the group answers rather than the screen
        // assuming it is the one starting (D042).
        groupSessionController.listenTogether { id ->
            audioGuideRequest(
                content,
                id,
                subtitle = content.trip.stories.firstOrNull { it.audioGuideId == id }?.title,
            )
        }
        // Leaving is deliberate and local-only: it stops following the group,
        // never the audio.
        onDispose { groupSessionController.leave() }
    }

    // 3 → 2 → 1, one second apart, matching the anchor published to the group.
    LaunchedEffect(group.isStarting) {
        while (groupSessionController.state.value.countdown != null) {
            delay(1_000L)
            groupSessionController.tickCountdown()
        }
    }

    // Screen 08 comes first: a phone joining a listen already in progress has
    // nothing loaded yet, and the guide it is about to hear is the group's.
    if (group.isStarting) {
        val startingId = mediaId ?: group.sharedMediaId
        val startingStory = content.trip.stories.firstOrNull { it.audioGuideId == startingId }
        SyncingScreen(
            storyTitle = startingStory?.title
                ?: content.audioGuide(startingId)?.title.orEmpty(),
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

    if (mediaId == null) {
        // Arriving here is the ask, and the ask is kept until there is a
        // listen to join (D045) — so a phone holding no guide is always
        // waiting on the group, whether the group is still being asked, is
        // paused, or has not started. One sentence for all three, and it says
        // what is being waited for rather than telling the traveller to go and
        // do something (D054).
        PlaceholderScreen(
            title = "Ouvir juntos",
            message = WAITING_FOR_GROUP,
            actionLabel = "Voltar",
            onAction = onBack,
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

    // The transport on screen 09 acts on the shared listen: the local player
    // first, then the group is told what it now says. Telling the group is
    // deliberately not a second way into this phone's player — the answer
    // comes back through the same correction as everyone else's (D047).
    fun shared(action: () -> Unit): () -> Unit = {
        action()
        groupSessionController.shareLocalPlayback()
    }

    ListenTogetherScreen(
        state = state,
        onBack = onBack,
        onTogglePlayPause = shared(playbackController::togglePlayPause),
        onSkipBack = shared { playbackController.seekBy(-SKIP_MILLIS) },
        onSkipForward = shared { playbackController.seekBy(SKIP_MILLIS) },
        // Offered only when a story actually backs this guide.
        onToggleTranscript = { showTranscript = !showTranscript }.takeIf { story != null },
        // Offered only while the walk still has somewhere to go.
        onNextStory = walkState.nextStop?.let { { onBack() } },
        modifier = modifier,
    )
}

/**
 * What screen 09 says while this phone has nothing to listen to yet.
 *
 * Written for a state the approved design does not draw, in the register of
 * the ones it does — a plain statement and then what happens next, no network,
 * no protocol, no waiting time. The traveller has just asked to listen
 * together; they are not being asked to do anything else (D054).
 */
private const val WAITING_FOR_GROUP =
    "Esperando o grupo. Assim que alguém começar, você entra junto."

/** ±15 on screen 09's transport, the same movement screen 07 uses (D025). */
private const val SKIP_MILLIS = 15_000L
