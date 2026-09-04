package com.travelcompanion.app.feature.more

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcStateDot
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.more.GroupMemberUi
import com.travelcompanion.app.domain.more.MoreUiState
import com.travelcompanion.app.domain.more.SavedContentUi
import com.travelcompanion.app.domain.more.formatBytes
import com.travelcompanion.app.feature.operations.ActionRow

/**
 * Screen 19 — Mais.
 *
 * Emergency first and unmistakable, then the three groups the approved sheet
 * names. Nothing here is live: the Grupo section reads the state the app
 * already holds rather than joining the group, because joining costs a write
 * every five seconds and D039 keeps that to screen 09 (D071).
 */
@Composable
fun MoreScreen(
    state: MoreUiState,
    onOpenEmergency: () -> Unit,
    onOpenPlanB: (String) -> Unit,
    onOpenAction: (ActionLink) -> Unit,
    onResetParticipant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text("Mais", style = TcType.titleEditorial, color = FieldCompanionColors.Ink)

            EmergencyRow(onOpenEmergency)

            Section("Na estrada") {
                // Frases úteis is the one row the sheet names that the package
                // cannot fill: the schema has no phrase list, only the single
                // sentence screen 17 shows a stranger. Left out rather than
                // invented, like the three of D069.
                state.apps.forEach { app ->
                    ActionRow(TcIcons.Compass, app.name) { onOpenAction(app.action) }
                }
                state.planBs.forEach { plan ->
                    ActionRow(TcIcons.Shield, "Plano B · ${plan.scenario}") { onOpenPlanB(plan.id) }
                }
            }

            Section("Grupo") {
                state.members.forEach { member -> MemberRow(member) }
                Text(
                    text = state.groupNote,
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }

            Section("Viagem e aparelho") {
                Card {
                    Text(state.tripTitle, style = TcType.subheadline, color = FieldCompanionColors.Ink)
                    state.tripSubtitle?.let {
                        Text(it, style = TcType.body, color = FieldCompanionColors.Neutral700)
                    }
                    Text(state.tripDates, style = TcType.meta, color = FieldCompanionColors.Neutral600)
                }
                Card {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Conteúdo salvo",
                            style = TcType.body.copy(fontWeight = FontWeight.Bold),
                            color = FieldCompanionColors.Ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = state.savedTotalLabel,
                            style = TcType.metaStrong,
                            color = FieldCompanionColors.Ink,
                        )
                    }
                    state.savedContent.forEach { line -> SavedLine(line) }
                }
                ActionRow(TcIcons.Group, "Trocar quem é você", onResetParticipant)
            }
        }
    }
}

/** The one row that is impossible to miss: oxblood, 68dp. */
@Composable
private fun EmergencyRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(TcCardShape)
            .background(FieldCompanionColors.Oxblood)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Abrir emergência" }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            TcIcons.Call,
            contentDescription = null,
            tint = FieldCompanionColors.White,
            modifier = Modifier.size(26.dp),
        )
        Text(
            text = "Emergência",
            style = TcType.action,
            color = FieldCompanionColors.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            TcIcons.ArrowRight,
            contentDescription = null,
            tint = FieldCompanionColors.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = TcType.eyebrow,
            color = FieldCompanionColors.Neutral600,
        )
        content()
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

/** S2 — always a dot *and* a word, never colour alone and never a number. */
@Composable
private fun MemberRow(member: GroupMemberUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(TcPillShape)
                .background(FieldCompanionColors.TealSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(member.initial, style = TcType.metaStrong, color = FieldCompanionColors.Teal)
        }
        Text(
            text = member.name,
            style = TcType.body,
            color = FieldCompanionColors.Ink,
            modifier = Modifier.weight(1f),
        )
        TcStateDot(
            color = when (member.sync) {
                ParticipantSync.Synchronized -> FieldCompanionColors.Teal
                ParticipantSync.Reconnecting -> FieldCompanionColors.GoldInk
                null -> FieldCompanionColors.Neutral400
            },
        )
        Text(member.label, style = TcType.meta, color = FieldCompanionColors.Neutral600)
    }
}

@Composable
private fun SavedLine(line: SavedContentUi) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${line.label} · ${line.count}",
            style = TcType.meta,
            color = FieldCompanionColors.Neutral600,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatBytes(line.bytes),
            style = TcType.meta,
            color = FieldCompanionColors.Neutral600,
        )
    }
}
