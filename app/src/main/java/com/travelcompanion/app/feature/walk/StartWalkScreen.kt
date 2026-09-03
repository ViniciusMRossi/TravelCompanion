package com.travelcompanion.app.feature.walk

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcAvatar
import com.travelcompanion.app.design.TcBottomActionBar
import com.travelcompanion.app.design.TcHairline
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcInkCard
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcStateDot
import com.travelcompanion.app.design.TcType

/**
 * Screen 06 — Iniciar passeio.
 *
 * The last screen before the phone goes into a pocket, so it answers one
 * question: is everything this walk needs already here? Every answer is read
 * from the device and the packaged trip, never from the network.
 */
@Composable
fun StartWalkScreen(
    state: StartWalkUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLocationSettings: (() -> Unit)? = null,
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
            Text("Passeio", style = TcType.action, color = FieldCompanionColors.Ink)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TcInkCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    text = state.eyebrow.uppercase(),
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.OnInkAccent,
                )
                Text(
                    text = state.title,
                    style = TcType.titleEditorial,
                    color = FieldCompanionColors.White,
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(state.distanceLabel, style = TcType.body, color = FieldCompanionColors.OnInkBody)
                    Text(state.durationLabel, style = TcType.body, color = FieldCompanionColors.OnInkBody)
                    Text(state.storyCountLabel, style = TcType.body, color = FieldCompanionColors.OnInkBody)
                }
                state.routeLabel?.let { route ->
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .clip(TcRouteShape)
                            .background(FieldCompanionColors.OnInkCard)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text("Rota", style = TcType.bodyStrong, color = FieldCompanionColors.White)
                        Text(
                            text = route,
                            style = TcType.body,
                            color = FieldCompanionColors.OnInkBody,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            ReadinessCard(
                state = state,
                // Only a way out when there is something to fix. Android turns
                // a second refusal into a permanent one and stops showing the
                // dialog at all, and it auto-revokes permissions for apps left
                // unused for months — which is exactly a trip app between
                // trips. Without this the row is a dead end.
                onOpenLocationSettings = onOpenLocationSettings.takeIf { !state.locationGranted },
            )

            Column {
                Text("Quem vai ouvir", style = TcType.sectionTitle, color = FieldCompanionColors.Ink)
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.participants.forEachIndexed { index, participant ->
                        ParticipantPill(
                            initial = participant.initial,
                            // "você" is the participant this phone belongs to;
                            // everyone else is simply along on the same walk.
                            label = participant.name + if (index == 0) " · você" else " · por perto",
                        )
                    }
                }
            }
        }

        TcHairline()
        TcBottomActionBar {
            TcPrimaryButton(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                minHeight = 56.dp,
            ) { Text("Começar passeio") }
            Text(
                text = "Pode guardar o celular no bolso. O passeio continua com a tela apagada.",
                style = TcType.meta,
                color = FieldCompanionColors.Neutral600,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ReadinessCard(state: StartWalkUiState, onOpenLocationSettings: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcRouteShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcRouteShape)
            .padding(horizontal = 16.dp),
    ) {
        state.readiness.forEachIndexed { index, line ->
            val fixable = index == 1 && onOpenLocationSettings != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (fixable) {
                            Modifier.clickable(onClick = onOpenLocationSettings!!)
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (index == 0) TcIcons.Headphones else TcIcons.Pin,
                    contentDescription = null,
                    tint = FieldCompanionColors.Teal,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = line.label,
                    style = TcType.body,
                    color = FieldCompanionColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                line.status?.let { StatusPill(it, line.statusTone) }
                if (fixable) {
                    // The chevron the design system already uses for "there is
                    // somewhere to go" (screen 01's cards, TcShortcutRow), so a
                    // status that can be acted on does not read as inert.
                    Icon(
                        imageVector = TcIcons.ChevronRight,
                        contentDescription = "Abrir configurações de localização",
                        tint = FieldCompanionColors.Neutral500,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            TcHairline()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = TcIcons.Offline,
                contentDescription = null,
                tint = FieldCompanionColors.Neutral600,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp),
            )
            Text(
                text = state.offlineNote,
                style = TcType.meta,
                color = FieldCompanionColors.Neutral700,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, tone: ReadinessTone) {
    val fill: Color
    val ink: Color
    when (tone) {
        ReadinessTone.Ready -> {
            fill = FieldCompanionColors.MossSoft
            ink = FieldCompanionColors.MossInk
        }
        ReadinessTone.Enabled -> {
            fill = FieldCompanionColors.TealSoft
            ink = FieldCompanionColors.TealDark
        }
        ReadinessTone.Muted -> {
            fill = FieldCompanionColors.Neutral100
            ink = FieldCompanionColors.Neutral700
        }
    }
    Row(
        modifier = Modifier
            .clip(TcPillShape)
            .background(fill)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (tone == ReadinessTone.Ready) TcStateDot(color = FieldCompanionColors.Moss)
        Text(text, style = TcType.label, color = ink)
    }
}

@Composable
private fun ParticipantPill(initial: String, label: String) {
    Row(
        modifier = Modifier
            .clip(TcPillShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcPillShape)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TcAvatar(initial = initial, size = 25.dp)
        TcStateDot(color = FieldCompanionColors.Moss)
        Text(label, style = TcType.meta, color = FieldCompanionColors.Ink)
    }
}

private val TcRouteShape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
