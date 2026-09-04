package com.travelcompanion.app.feature.fullday

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCard
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.fullday.DayPeriodSection
import com.travelcompanion.app.domain.fullday.DayStayUi
import com.travelcompanion.app.domain.fullday.DayTransportUi
import com.travelcompanion.app.domain.fullday.FullDayUiState
import com.travelcompanion.app.domain.today.ShortcutUi
import com.travelcompanion.app.feature.today.CriticalCard
import com.travelcompanion.app.feature.today.ShortcutRow
import com.travelcompanion.app.feature.today.TimelineRow
import java.time.LocalDate

/**
 * Screen 03 — Dia completo.
 *
 * The same day as Today, read end to end. The critical item comes first and
 * summarised, with the two buttons Today gives it — somebody opening this at
 * 18:00 must not have to scroll to find the bus — and only then the day in
 * morning, afternoon and evening.
 */
@Composable
fun FullDayScreen(
    state: FullDayUiState,
    onBack: (() -> Unit)?,
    onGoToDate: (LocalDate) -> Unit,
    onOpenMaps: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenPlanB: (String) -> Unit,
    onOpenTransport: (String) -> Unit,
    onOpenStay: (String) -> Unit,
    onOpenTimelineItem: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        DayHeader(state, onBack, onGoToDate)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            state.critical?.let { critical ->
                CriticalCard(
                    critical = critical,
                    onOpenMaps = onOpenMaps,
                    // Same `document:` uri Today reads, same way in.
                    onShowDocument = {
                        critical.documentAction?.uri
                            ?.substringAfter("document:")
                            ?.takeIf { it.isNotBlank() }
                            ?.let(onOpenDocument)
                    },
                )
            }

            state.sections.forEach { section -> PeriodSection(section, onOpenTimelineItem) }

            state.transports.forEach { transport ->
                TransportCard(transport) { onOpenTransport(transport.id) }
            }
            state.stays.forEach { stay -> StayCard(stay) { onOpenStay(stay.id) } }

            if (state.shortcuts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DESTE DIA",
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.Neutral600,
                    )
                    state.shortcuts.forEach { shortcut ->
                        ShortcutRow(shortcut) {
                            when (shortcut.kind) {
                                ShortcutUi.Kind.Document -> onOpenDocument(shortcut.id)
                                ShortcutUi.Kind.PlanB -> onOpenPlanB(shortcut.id)
                                ShortcutUi.Kind.Memory -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Date, "Dia 9 de 21 · Sarajevo", and the two arrows.
 *
 * An arrow with no day behind it is drawn and dimmed rather than removed: the
 * header must not change shape at the ends of the trip, and there is no day 0
 * and no day 22 to reach.
 */
@Composable
private fun DayHeader(
    state: FullDayUiState,
    onBack: (() -> Unit)?,
    onGoToDate: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        onBack?.let { back ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(TcPillShape)
                        .clickable(onClick = back)
                        .semantics { contentDescription = "Voltar para Hoje" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = TcIcons.ArrowLeft,
                        contentDescription = null,
                        tint = FieldCompanionColors.Ink,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text("Hoje", style = TcType.action, color = FieldCompanionColors.Ink)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)) {
                Text(state.dateLabel, style = TcType.titleEditorial, color = FieldCompanionColors.Ink)
                Text(state.dayLabel, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
            DayArrow(TcIcons.ArrowLeft, "Dia anterior", state.previousDate, onGoToDate)
            DayArrow(TcIcons.ArrowRight, "Próximo dia", state.nextDate, onGoToDate)
        }
    }
}

@Composable
private fun DayArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    date: LocalDate?,
    onGoToDate: (LocalDate) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(TcPillShape)
            .alpha(if (date != null) 1f else 0.3f)
            .then(
                if (date != null) {
                    Modifier
                        .clickable { onGoToDate(date) }
                        .semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FieldCompanionColors.Ink,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PeriodSection(section: DayPeriodSection, onOpenTimelineItem: (String, String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.period.label.uppercase(),
            style = TcType.eyebrow,
            color = FieldCompanionColors.Neutral600,
        )
        TcCard(modifier = Modifier.fillMaxWidth()) {
            section.rows.forEach { row ->
                TimelineRow(row) { opened ->
                    opened.item.refId?.let { onOpenTimelineItem(opened.item.kind, it) }
                }
            }
        }
    }
}

/** The end-of-day transport, with its times at the approved 28px. */
@Composable
private fun TransportCard(transport: DayTransportUi, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("FIM DO DIA", style = TcType.eyebrow, color = FieldCompanionColors.Neutral600)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegEnd(transport.originTime, transport.originName, Modifier.weight(1f))
            LegEnd(transport.destinationTime, transport.destinationName, Modifier.weight(1f))
        }
        Text(
            text = listOfNotNull(
                transport.operator,
                transport.platform?.let { "Plataforma $it" },
            ).joinToString(" · "),
            style = TcType.meta,
            color = FieldCompanionColors.Neutral600,
        )
        TcSecondaryButton(onClick = onOpen) { Text("Ver transporte") }
    }
}

@Composable
private fun LegEnd(time: String, name: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = time, style = TcType.clockLarge, color = FieldCompanionColors.Ink)
        Text(text = name, style = TcType.body, color = FieldCompanionColors.Neutral700)
    }
}

@Composable
private fun StayCard(stay: DayStayUi, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .clickable(onClick = onOpen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ONDE DORMIR", style = TcType.eyebrow, color = FieldCompanionColors.Neutral600)
        Text(stay.name, style = TcType.subheadline, color = FieldCompanionColors.Ink)
        stay.checkIn?.let {
            Text("Check-in a partir de $it", style = TcType.body, color = FieldCompanionColors.Neutral700)
        }
        TcSecondaryButton(onClick = onOpen) { Text("Ver hospedagem") }
    }
}
