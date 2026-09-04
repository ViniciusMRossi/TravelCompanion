package com.travelcompanion.app.feature.planb

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.PlanBStep
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.operations.PhoneUi
import com.travelcompanion.app.feature.operations.ActionRow
import com.travelcompanion.app.feature.operations.DetailHeader
import com.travelcompanion.app.feature.operations.PhoneRow
import com.travelcompanion.app.feature.operations.SoftCard

/**
 * Screen 18 — Plano B.
 *
 * Calm on purpose: the scenario named in human language, then the promise that
 * nobody sleeps in the street, then the steps in the order they are worth
 * trying. Nothing here is styled as an alarm — the alarm already happened.
 */
@Composable
fun PlanBScreen(
    state: PlanBUiState,
    onBack: () -> Unit,
    onAction: (ActionLink) -> Unit,
    onDial: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        DetailHeader(title = "Plano B", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = state.scenario,
                    style = TcType.titleEditorial,
                    color = FieldCompanionColors.Ink,
                )
                Text(
                    text = state.reassurance,
                    style = TcType.editorialBody,
                    color = FieldCompanionColors.Neutral700,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.steps.forEachIndexed { index, step ->
                    StepCard(
                        number = index + 1,
                        step = step.step,
                        isCurrent = step.isCurrent,
                        phones = state.phones,
                        onAction = onAction,
                        onDial = onDial,
                    )
                }
            }

            if (state.alternatives.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "JÁ GUARDADO",
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.Neutral600,
                    )
                    state.alternatives.forEach { alternative ->
                        ActionRow(TcIcons.Document, alternative.title) {
                            onOpenDocument(alternative.documentId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    step: PlanBStep,
    isCurrent: Boolean,
    phones: Map<String, PhoneUi>,
    onAction: (ActionLink) -> Unit,
    onDial: (String) -> Unit,
) {
    SoftCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(TcPillShape)
                    .background(
                        if (isCurrent) FieldCompanionColors.Teal else FieldCompanionColors.Neutral100,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$number",
                    style = TcType.metaStrong,
                    color = if (isCurrent) {
                        FieldCompanionColors.White
                    } else {
                        FieldCompanionColors.Neutral700
                    },
                )
            }
            Text(
                text = step.title,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
                modifier = Modifier.weight(1f),
            )
            step.deadline?.let {
                Text(
                    text = "até $it",
                    style = TcType.metaStrong,
                    color = FieldCompanionColors.Oxblood,
                )
            }
        }
        Text(step.instruction, style = TcType.body, color = FieldCompanionColors.Neutral800)
        step.expectedOutcome?.let {
            Text(
                text = "O que esperar: $it",
                style = TcType.meta,
                color = FieldCompanionColors.Neutral600,
            )
        }
        step.actions.forEach { action ->
            val phone = phones[action.uri]
            if (action.kind == "phone" && phone != null) {
                PhoneRow(phone, onDial)
            } else {
                ActionRow(TcIcons.Map, action.label) { onAction(action) }
            }
        }
    }
}
