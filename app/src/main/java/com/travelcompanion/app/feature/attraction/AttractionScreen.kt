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
import com.travelcompanion.app.domain.editorial.EditorialSectionUi

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

            // Narration sits between the summary and the operational strip on
            // purpose. The approved sheet keeps editorial and operational
            // apart — "o horário nunca fica dentro do parágrafo editorial" —
            // and the rule holds in both directions: the long text does not
            // drift down beside the departure time either (D133).
            state.historySections.forEach { section -> EditorialSectionBlock(section) }

            state.departure?.let { departure -> DepartureStrip(departure) }

            if (state.practicalLines.isNotEmpty()) {
                PracticalBlock(state.practicalLines)
            }

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
            // The city line, the name and the subtitle sit on the photograph.
            titleOverPhotograph = true,
            imageAssetPath = state.heroAssetPath,
            placeholderCaption = state.heroCaption,
            cool = true,
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    // The top inset is the placeholder caption's band. This
                    // hero takes its height from its own content above a 200dp
                    // floor, and at 360dp with the font at 1.5 the city line
                    // grew to two lines and rose straight into the caption —
                    // both unreadable. Reserving the band makes the hero grow
                    // instead of overlapping; it costs nothing once a real
                    // photograph arrives and the caption goes (D085).
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 56.dp),
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
 * One titled stretch of narration.
 *
 * Each paragraph is its own `Text`, spaced like the rest of the screen: the
 * body arrives as a single field and would otherwise be one slab of three
 * hundred words. Nothing is drawn for an attraction with no sections — no
 * heading, no reserved space — which is the same rule as the missing
 * photograph and screen 20's absent "Para pedir" box (D133).
 */
@Composable
private fun EditorialSectionBlock(section: EditorialSectionUi) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.title,
            style = TcType.sectionTitle,
            color = FieldCompanionColors.Ink,
        )
        section.paragraphs.forEach { paragraph ->
            Text(
                text = paragraph,
                style = TcType.editorialBody,
                color = FieldCompanionColors.Neutral700,
            )
        }
    }
}

/**
 * What the ticket costs and when the gate opens, in the source's own words.
 *
 * It sits in the operational layer, beside the departure strip and the map
 * buttons and after the editorial sections, because it is what gets read
 * standing at a counter. Nothing here is truncated: these strings run to 141
 * characters in the packaged trip and the instruction is always in the tail
 * — "mas não cobre o teleférico", "Levem os €10", "só em dinheiro: não
 * aceitam cartão nem euro". Screen 05 draws `practical` here and nowhere
 * else, so a cut here is a cut with no second chance (D154).
 */
@Composable
private fun PracticalBlock(lines: List<PracticalLineUi>) {
    TcCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            lines.forEach { line ->
                Column {
                    Text(
                        text = line.label.uppercase(),
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.Neutral600,
                    )
                    Text(
                        text = line.value,
                        style = TcType.meta,
                        color = FieldCompanionColors.Neutral800,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
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
