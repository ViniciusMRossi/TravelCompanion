package com.travelcompanion.app.feature.attraction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcAudioButton
import com.travelcompanion.app.design.TcCard
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcHairline
import com.travelcompanion.app.design.TcHero
import com.travelcompanion.app.design.TcHeroShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcNumberedNotes
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcPlanBCard
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType

/**
 * Screen 05 — Atração.
 *
 * The record for one place, plus the bridge into the walk. Editorial copy and
 * operational timing stay in separate blocks: the departure time never lives
 * inside the paragraph.
 */
@Composable
fun AttractionScreen(
    state: AttractionUiState,
    onBack: () -> Unit,
    onPlayAudioGuide: () -> Unit,
    onOpenMaps: (String) -> Unit,
    onDirections: (String, String?) -> Unit,
    onStartWalk: (String) -> Unit,
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
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TcPillShape)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TcIcons.ArrowLeft,
                    contentDescription = null,
                    tint = FieldCompanionColors.Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text("Atração", style = TcType.action, color = FieldCompanionColors.Ink)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HeroBlock(state)

            state.departure?.let { departure -> DepartureStrip(departure) }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AudioGuideAction(state = state, onPlayAudioGuide = onPlayAudioGuide)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.mapsAction?.let { action ->
                        TcSecondaryButton(
                            onClick = { onOpenMaps(action.uri) },
                            modifier = Modifier.weight(1f),
                            icon = TcIcons.Map,
                        ) { Text(action.label) }
                    }
                    TcSecondaryButton(
                        onClick = { onDirections(state.directions.uri, state.directions.fallbackUri) },
                        modifier = Modifier.weight(1f),
                    ) { Text(state.directions.label) }
                }
            }

            if (state.whatToObserve.isNotEmpty()) {
                Column {
                    Text(
                        text = "O que observar",
                        style = TcType.sectionTitle,
                        color = FieldCompanionColors.Ink,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    TcNumberedNotes(state.whatToObserve)
                }
            }

            if (state.planBTitle != null && state.planBBody != null) {
                TcPlanBCard(
                    title = state.planBTitle,
                    body = state.planBBody,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // The fixed bar exists to start the walk that leaves from here; with no
        // such walk in the packaged day there is nothing for it to start.
        state.departure?.let { departure ->
            TcHairline()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FieldCompanionColors.Surface)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
            ) {
                TcPrimaryButton(
                    onClick = { onStartWalk(departure.walkId) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = TcIcons.NearMe,
                    minHeight = 56.dp,
                ) { Text("Iniciar passeio") }
            }
        }
    }
}

/**
 * The approved audioguide action.
 *
 * Screen 05 specifies one action — "Ouvir audioguia · N min" — and that is all
 * it is. Playback itself surfaces in the persistent compact player, which is
 * the design system's answer for audio outside the shared-player screen; the
 * full transport belongs to screens 07 and 09.
 *
 * When the audio file is not packaged the action is replaced by a plain
 * statement, because offering "Ouvir" for audio that is not on the device is
 * the same broken promise as badging a missing document "Offline".
 */
@Composable
private fun AudioGuideAction(
    state: AttractionUiState,
    onPlayAudioGuide: () -> Unit,
) {
    val label = state.audioLabel ?: return

    if (!state.audioAvailableOffline) {
        TcCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Audioguia".uppercase(),
                style = TcType.eyebrow,
                color = FieldCompanionColors.Neutral600,
            )
            Text(
                text = "O áudio deste guia ainda não está salvo neste aparelho.",
                style = TcType.meta,
                color = FieldCompanionColors.Neutral700,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        return
    }

    TcAudioButton(
        onClick = onPlayAudioGuide,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(label) }
}

/** Editorial hero: photography, name in Fraunces, then the opening paragraph. */
@Composable
private fun HeroBlock(state: AttractionUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcHeroShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcHeroShape),
    ) {
        TcHero(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            imageAssetPath = state.heroAssetPath,
            placeholderCaption = state.heroCaption,
            cool = true,
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            ) {
                Text(
                    text = state.cityLine.uppercase(),
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.White.copy(alpha = 0.85f),
                )
                Text(
                    text = state.name,
                    style = TcType.heroEditorial,
                    color = FieldCompanionColors.White,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                state.subtitle?.let {
                    Text(text = it, style = TcType.meta, color = FieldCompanionColors.White)
                }
            }
        }

        Column(modifier = Modifier.padding(18.dp)) {
            if (state.chips.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.chips.forEachIndexed { index, chip ->
                        TcChip(
                            text = chip,
                            tone = if (index == 0) TcChipTone.Teal else TcChipTone.Neutral,
                        )
                    }
                }
            }
            Text(
                text = state.summary,
                style = TcType.editorialBody,
                color = FieldCompanionColors.Neutral800,
            )
        }
    }
}

/**
 * The operational strip. It is a separate block on purpose — this is the one
 * thing on the screen that has a deadline attached to it.
 */
@Composable
private fun DepartureStrip(departure: WalkDepartureUi) {
    TcCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = departure.time,
                style = TcType.clockMedium,
                color = FieldCompanionColors.Ink,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = departure.title,
                    style = TcType.bodyStrong,
                    color = FieldCompanionColors.Ink,
                )
                val detail = listOfNotNull(
                    departure.meetingPoint?.let { "Encontro em $it" },
                    departure.storyCount.takeIf { it > 0 }?.let { "$it histórias" },
                ).joinToString(" · ")
                if (detail.isNotBlank()) {
                    Text(text = detail, style = TcType.meta, color = FieldCompanionColors.Neutral600)
                }
            }
        }
    }
}
