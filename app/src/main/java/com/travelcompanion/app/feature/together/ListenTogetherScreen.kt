package com.travelcompanion.app.feature.together

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcAudioPlayer
import com.travelcompanion.app.design.TcAudioPlayerVariant
import com.travelcompanion.app.design.TcAvatar
import com.travelcompanion.app.design.TcHero
import com.travelcompanion.app.design.TcHeroShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcStateDot
import com.travelcompanion.app.design.TcType

/** One row of screen 09's "Ouvindo juntos" list. */
data class ListenerUi(
    val initial: String,
    val name: String,
    /** Which dot: green for in step, amber for not. */
    val sync: ParticipantSync,
    /** What the row says. Built with the state, so the two cannot disagree. */
    val label: String,
)

/**
 * Screen 09 — Ouvir juntos.
 *
 * The bright-screen counterpart to Walk Mode: for when people have stopped and
 * are looking at the phone together. It uses the design system's
 * [TcAudioPlayerVariant.Full] teal card at 52 / 68 / 52dp, which is this
 * screen's transport — screen 07's ink one is a different thing (D029).
 *
 * The group is described by its result and by people's names. There is no
 * network name, no signal, no host or client, and the word "offline" never
 * appears: when the group cannot be reached, the screen says so in the
 * approved sentence and points out that the audioguide is unaffected.
 */
@Composable
fun ListenTogetherScreen(
    state: ListenTogetherUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onToggleTranscript: (() -> Unit)?,
    onNextStory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TcPillShape)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Voltar ao passeio" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TcIcons.ArrowLeft,
                    contentDescription = null,
                    tint = FieldCompanionColors.Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text("Ouvindo juntos", style = TcType.action, color = FieldCompanionColors.Ink)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TcHero(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(TcHeroShape),
                imageAssetPath = state.heroAssetPath,
                placeholderCaption = state.heroCaption,
                cool = true,
            ) {
                Text(
                    text = state.storyTitle,
                    style = TcType.subheadline,
                    color = FieldCompanionColors.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                )
            }

            TcAudioPlayer(
                title = state.chapterLabel,
                subtitle = state.walkTitle,
                progress = state.progress,
                elapsed = state.elapsed,
                total = state.total,
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                variant = TcAudioPlayerVariant.Full,
                onTogglePlayPause = onTogglePlayPause,
                onSkipBack = onSkipBack,
                onSkipForward = onSkipForward,
                modifier = Modifier.fillMaxWidth(),
            )

            ListenersCard(state)

            // The row disappears entirely rather than showing an empty strip:
            // a guide with no story behind it has no transcript, and the last
            // stop of a walk has no next story.
            if (onToggleTranscript != null || onNextStory != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onToggleTranscript != null) {
                        TcSecondaryButton(
                            onClick = onToggleTranscript,
                            modifier = Modifier
                                .weight(1f)
                                // The web reference carries aria-expanded; this
                                // is the Android equivalent, so a screen reader
                                // announces the disclosure rather than reading a
                                // label that changed for no stated reason.
                                .semantics {
                                    stateDescription =
                                        if (state.showTranscript) "Expandido" else "Recolhido"
                                },
                        ) { Text(if (state.showTranscript) "Ocultar texto" else "Transcrição") }
                    }
                    // Rendered only when there is somewhere to go. The design
                    // system has no disabled secondary button, and a control
                    // that does nothing is worse than one that is not there.
                    if (onNextStory != null) {
                        TcSecondaryButton(
                            onClick = onNextStory,
                            modifier = Modifier.weight(1f),
                        ) { Text("Próxima história") }
                    }
                }
            }

            // Below the trigger, never above it: entering above would push the
            // collapse button out of view at the moment it appears.
            AnimatedVisibility(visible = state.showTranscript) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Transcrição da história" }
                        .clip(RoundedCornerShape(14.dp))
                        .background(FieldCompanionColors.Surface)
                        .border(1.dp, FieldCompanionColors.Neutral200, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    Text(
                        text = state.transcript.orEmpty(),
                        style = TcType.editorialBody,
                        color = FieldCompanionColors.Neutral800,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListenersCard(state: ListenTogetherUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "OUVINDO JUNTOS",
            style = TcType.eyebrow,
            color = FieldCompanionColors.Neutral600,
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.listeners.forEach { listener ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TcAvatar(initial = listener.initial, size = 32.dp)
                    // The name is measured first and keeps its line; the state
                    // beside it yields (D055). It used to be the other way
                    // round — the state took its full width and the name wrapped
                    // — so at 360dp "Vinícius · você" broke across two lines to
                    // make room for "Sincronizando novamente". A name has no
                    // substitute; the word does, because the dot next to it says
                    // the same thing.
                    Text(
                        text = listener.name,
                        style = TcType.body,
                        color = FieldCompanionColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        TcStateDot(
                            color = when (listener.sync) {
                                ParticipantSync.Synchronized -> FieldCompanionColors.Moss
                                ParticipantSync.Reconnecting -> FieldCompanionColors.Gold
                            },
                        )
                        Text(
                            text = listener.label,
                            style = TcType.meta,
                            color = when (listener.sync) {
                                ParticipantSync.Synchronized -> FieldCompanionColors.MossInk
                                ParticipantSync.Reconnecting -> FieldCompanionColors.GoldInk
                            },
                            // The dot is measured before this and always
                            // survives: it is the state, said without words.
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (state.groupNote != null) {
            // The whole point of §3.3 said in the traveller's language: the
            // group is what failed, not the audio.
            Text(
                text = state.groupNote,
                style = TcType.meta,
                color = FieldCompanionColors.GoldBody,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FieldCompanionColors.GoldSoft)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}
