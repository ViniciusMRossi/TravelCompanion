package com.travelcompanion.app.feature.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.today.CriticalItemUi
import com.travelcompanion.app.domain.today.TimelineRowUi

/**
 * Screen 11 — Fim do passeio.
 *
 * Closes the walk and hands the traveller back to the day. Everything above
 * "Ainda hoje" is the walk that just ended; everything below is operation, and
 * the critical item keeps its oxblood and its instruction, because finishing a
 * walk at 18:40 is exactly when the bus at 19:30 matters most.
 */
@Composable
fun WalkFinishedScreen(
    state: WalkFinishedUiState,
    onRecordMemory: () -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TcChip(text = "Passeio concluído", tone = TcChipTone.Moss)
            Text(
                text = state.title,
                style = TcType.titleEditorial.copy(fontSize = 36.sp),
                color = FieldCompanionColors.Ink,
            )
            Text(
                text = state.endedLine,
                style = TcType.body,
                color = FieldCompanionColors.Neutral700,
            )
        }

        // Four cards, two by two: at 360dp with the font at 1.5 a single row of
        // four is unreadable, and wrapping keeps every label whole (D072).
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            state.cards.forEach { card ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(TcCardShape)
                        .background(FieldCompanionColors.Surface)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = card.label.uppercase(),
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.Neutral600,
                    )
                    Text(
                        text = card.value,
                        style = TcType.body.copy(fontWeight = FontWeight.Bold),
                        color = FieldCompanionColors.Ink,
                    )
                }
            }
        }

        if (state.critical != null || state.remaining.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AINDA HOJE",
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Neutral600,
                )
                state.critical?.let { CriticalLine(it) }
                state.remaining.forEach { row -> RemainingLine(row) }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TcPrimaryButton(onClick = onRecordMemory) { Text("Gravar memória") }
            TcSecondaryButton(onClick = onBackToToday) { Text("Voltar para Hoje") }
        }
    }
}

/** The one thing that cannot go wrong, still in oxblood. */
@Composable
private fun CriticalLine(critical: CriticalItemUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.CriticalSoft)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            critical.nominalTime?.let {
                Text(text = it, style = TcType.clockSmall, color = FieldCompanionColors.Oxblood)
            }
            Text(
                text = critical.item.title,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
            )
        }
        critical.item.instruction?.let {
            Text(it, style = TcType.meta, color = FieldCompanionColors.CriticalBody)
        }
    }
}

@Composable
private fun RemainingLine(row: TimelineRowUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.item.startTime,
            style = TcType.clockSmall,
            color = FieldCompanionColors.Ink,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(row.item.title, style = TcType.body, color = FieldCompanionColors.Ink)
            row.item.detail?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
        }
    }
}
