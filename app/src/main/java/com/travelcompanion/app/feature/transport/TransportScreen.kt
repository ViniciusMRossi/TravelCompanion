package com.travelcompanion.app.feature.transport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.feature.operations.ActionRow
import com.travelcompanion.app.feature.operations.CriticalBlock
import com.travelcompanion.app.feature.operations.DetailHeader
import com.travelcompanion.app.feature.operations.Meta
import com.travelcompanion.app.feature.operations.PhoneRow
import com.travelcompanion.app.feature.operations.SoftCard

/**
 * Screen 15 — Transporte.
 *
 * Critical timing first and apart from everything editorial: "Esteja na
 * estação até 19:00" is not the departure time, and the approved screen never
 * lets one stand in for the other.
 */
@Composable
fun TransportScreen(
    state: TransportUiState,
    onBack: () -> Unit,
    onAction: (ActionLink) -> Unit,
    onDial: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenPlanB: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        DetailHeader(title = "Transporte", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            state.critical?.let { critical ->
                CriticalBlock(item = critical, window = state.window) {
                    // Wrapping rather than shrinking: at 360dp with the system
                    // font at 1.5 these two labels do not fit side by side, and
                    // a Row squeezed them to one character per line. An action
                    // keeps its whole label and takes the next line instead —
                    // the same treatment screen 06 gives its participant pills
                    // (D055's question, answered for buttons).
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.documentId?.let { id ->
                            TcSecondaryButton(onClick = { onOpenDocument(id) }) {
                                Text("Mostrar passagem")
                            }
                        }
                        state.criticalActions.firstOrNull { it.kind == "maps" }?.let { action ->
                            TcSecondaryButton(onClick = { onAction(action) }) { Text(action.label) }
                        }
                    }
                }
            }

            Journey(state)

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                state.durationLabel?.let { Meta("Duração", it, Modifier.weight(1f)) }
                state.peopleLabel?.let { Meta("Pessoas", it, Modifier.weight(1f)) }
                state.price?.let { Meta("Valor", it, Modifier.weight(1f)) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.documentId?.let { id ->
                    ActionRow(TcIcons.Ticket, "Bilhete") { onOpenDocument(id) }
                }
                state.actions.forEach { action ->
                    ActionRow(TcIcons.Map, action.label) { onAction(action) }
                }
                state.operatorPhone?.let { PhoneRow(it, onDial) }
            }

            state.planB?.let { plan ->
                SoftCard(onClick = { onOpenPlanB(plan.id) }) {
                    Text("PLANO B", style = TcType.eyebrow, color = FieldCompanionColors.Neutral600)
                    Text(
                        text = plan.scenario,
                        style = TcType.body.copy(fontWeight = FontWeight.Bold),
                        color = FieldCompanionColors.Ink,
                    )
                    Text(
                        text = plan.reassurance,
                        style = TcType.meta,
                        color = FieldCompanionColors.Neutral600,
                    )
                }
            }
        }
    }
}

/** The vertical journey: two ends and the run between them. */
@Composable
private fun Journey(state: TransportUiState) {
    SoftCard {
        // The artboard names the leg here, above the journey: "Ônibus ·
        // Centrotrans". The app had never drawn the row, so the leg was named
        // only by its two station names — and the service number the package
        // declares was on no screen at all (D177).
        Text(
            text = state.serviceLine.uppercase(),
            style = TcType.eyebrow,
            color = FieldCompanionColors.Neutral600,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LegEnd(state.origin)
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .width(2.dp)
                .height(26.dp)
                .background(FieldCompanionColors.Neutral200),
        )
        LegEnd(state.destination)
    }
}

@Composable
private fun LegEnd(end: LegEndUi) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .size(10.dp)
                .clip(TcPillShape)
                .background(FieldCompanionColors.Teal),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = end.time,
                style = TcType.clockLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
            )
            Text(end.stationName, style = TcType.body, color = FieldCompanionColors.Neutral700)
            end.platform?.let {
                Text(
                    text = "Plataforma $it",
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
        }
    }
}
