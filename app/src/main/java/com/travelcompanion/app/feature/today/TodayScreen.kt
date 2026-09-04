package com.travelcompanion.app.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.data.trip.Outfit
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCard
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcInkCard
import com.travelcompanion.app.design.TcOfflineChip
import com.travelcompanion.app.design.TcOnInkPrimaryButton
import com.travelcompanion.app.design.TcOnInkSecondaryButton
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcSectionHeader
import com.travelcompanion.app.design.TcShortcutRow
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.today.BookingStatusUi
import com.travelcompanion.app.domain.today.CriticalItemUi
import com.travelcompanion.app.domain.today.NowBlockUi
import com.travelcompanion.app.domain.today.ShortcutUi
import com.travelcompanion.app.domain.today.TimelineRowUi
import com.travelcompanion.app.domain.today.TimelineState
import com.travelcompanion.app.domain.today.TodayUiState
import com.travelcompanion.app.domain.today.WeatherUi

/**
 * Screen 02 — Hoje.
 *
 * Answers, in this order: what is now, what cannot go wrong today, what to
 * wear, and the whole day. Everything on it comes from packaged content, so
 * it renders identically with no network.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onOpenAttraction: (String) -> Unit,
    onOpenMaps: (String) -> Unit,
    onOpenFullDay: () -> Unit,
    onOpenShortcut: (ShortcutUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        TodayHeader(state)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = state.dateLabel.uppercase(),
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Neutral600,
                )
                Text(
                    text = state.title,
                    style = TcType.titleEditorial,
                    color = FieldCompanionColors.Ink,
                )
            }

            state.now?.let { now ->
                NowCard(
                    now = now,
                    onOpenAttraction = onOpenAttraction,
                    onOpenMaps = onOpenMaps,
                )
            }

            state.criticalItems.forEach { critical ->
                CriticalCard(
                    critical = critical,
                    onOpenMaps = onOpenMaps,
                    onShowDocument = { onOpenShortcut(documentShortcutFor(critical)) },
                )
            }

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WeatherCard(state.weather, Modifier.weight(1f).fillMaxHeight())
                OutfitCard(state.outfit, Modifier.weight(1f).fillMaxHeight())
            }

            TcSectionHeader(
                title = "O dia inteiro",
                modifier = Modifier.padding(top = 4.dp),
                actionLabel = "Ver dia completo",
                onAction = onOpenFullDay,
            )

            if (state.timeline.isNotEmpty()) {
                TimelineCard(state.timeline)
            }

            if (state.shortcuts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.shortcuts.forEach { shortcut ->
                        ShortcutRow(shortcut) { onOpenShortcut(shortcut) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(state: TodayUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = TcIcons.Pin,
                    contentDescription = null,
                    tint = FieldCompanionColors.Teal,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = listOfNotNull(state.cityName, state.countryName).joinToString(" · "),
                    style = TcType.metaStrong,
                    color = FieldCompanionColors.Ink,
                    // One line, as the approved header draws it. The chip
                    // beside it keeps its own width, so on a narrow screen —
                    // and far worse at a large system font — what is left for
                    // this line can be a few dp, and a `Text` given a few dp
                    // does not shrink: it wraps, one character per line, for
                    // as many lines as the name has letters. Screen 02 was a
                    // vertical alphabet at 360dp with the font at 1.5 (D051).
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = state.dayLabel,
                style = TcType.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                color = FieldCompanionColors.Neutral600,
                // One line, for the same reason as the line above it.
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TcOfflineChip(state.offlineNote)
    }
}

/**
 * The single dark block on Today. Carries the current commitment and the
 * next one inside the same sentence, exactly as approved.
 */
@Composable
private fun NowCard(
    now: NowBlockUi,
    onOpenAttraction: (String) -> Unit,
    onOpenMaps: (String) -> Unit,
) {
    TcInkCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (now.isActive) "Agora · ${now.time}" else "A seguir · ${now.time}",
            style = TcType.eyebrow,
            color = FieldCompanionColors.OnInkEyebrow,
        )
        Text(
            text = now.time,
            style = TcType.clockLarge,
            color = FieldCompanionColors.White,
            modifier = Modifier.padding(vertical = 9.dp),
        )
        Text(
            text = now.title,
            style = TcType.headline,
            color = FieldCompanionColors.White,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        val paragraph = listOfNotNull(now.detail, now.nextLine).joinToString(" ")
        if (paragraph.isNotBlank()) {
            Text(
                text = paragraph,
                style = TcType.body,
                color = FieldCompanionColors.OnInkBody,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        now.audioLine?.let { line ->
            Row(
                modifier = Modifier.padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = TcIcons.Headphones,
                    contentDescription = null,
                    tint = FieldCompanionColors.OnInkMeta,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = line,
                    style = TcType.meta.copy(fontSize = 13.sp),
                    color = FieldCompanionColors.OnInkMeta,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            now.attractionId?.let { id ->
                TcOnInkPrimaryButton(onClick = { onOpenAttraction(id) }) { Text("Ver atração") }
            }
            now.mapsAction?.let { action ->
                TcOnInkSecondaryButton(onClick = { onOpenMaps(action.uri) }) { Text(action.label) }
            }
        }
    }
}

/**
 * "Não pode dar errado".
 *
 * The nominal time and the deadline the traveller must actually meet are
 * rendered as two separate things; the instruction never hides inside prose.
 */
@Composable
private fun CriticalCard(
    critical: CriticalItemUi,
    onOpenMaps: (String) -> Unit,
    onShowDocument: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(TcCardShape)
            .background(FieldCompanionColors.CriticalSoft)
            .border(1.dp, FieldCompanionColors.CriticalBorder, TcCardShape),
    ) {
        // 5dp oxblood spine, as approved. Criticality is a band, not a status swap.
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(FieldCompanionColors.Oxblood),
        )
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = TcIcons.Warning,
                    contentDescription = null,
                    tint = FieldCompanionColors.Oxblood,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Não pode dar errado".uppercase(),
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Oxblood,
                )
            }
            critical.nominalTime?.let { time ->
                Text(
                    text = time,
                    style = TcType.clockMedium,
                    color = FieldCompanionColors.Ink,
                    modifier = Modifier.padding(top = 7.dp, bottom = 3.dp),
                )
            }
            Text(
                text = critical.item.title,
                style = TcType.subheadline,
                color = FieldCompanionColors.Ink,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = listOfNotNull(critical.item.instruction, critical.item.reason).joinToString(" "),
                style = TcType.body,
                color = FieldCompanionColors.CriticalBody,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                critical.documentAction?.let { action ->
                    TcPrimaryButton(onClick = onShowDocument) { Text(action.label) }
                }
                critical.mapsAction?.let { action ->
                    TcSecondaryButton(onClick = { onOpenMaps(action.uri) }) { Text(action.label) }
                }
            }
        }
    }
}

/**
 * Weather never blocks Today, and freshness is always stated rather than
 * implied: a packaged forecast says so instead of posing as live data.
 */
@Composable
private fun WeatherCard(weather: WeatherUi, modifier: Modifier = Modifier) {
    TcCard(modifier = modifier) {
        when (weather) {
            is WeatherUi.FallbackFromTrip -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TcIcons.Sun,
                        contentDescription = null,
                        tint = FieldCompanionColors.Gold,
                        modifier = Modifier.size(24.dp),
                    )
                    weather.maxC?.let {
                        Text(
                            text = "${it.toInt()}°",
                            style = TcType.clockMedium.copy(fontSize = 28.sp),
                            color = FieldCompanionColors.Ink,
                        )
                    }
                }
                val lines = listOfNotNull(
                    weather.summary,
                    range(weather.minC, weather.maxC),
                    weather.rainNote,
                    weather.windNote,
                )
                if (lines.isNotEmpty()) {
                    Text(
                        text = lines.joinToString("\n"),
                        style = TcType.meta,
                        color = FieldCompanionColors.Neutral700,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TcIcons.Offline,
                        contentDescription = null,
                        tint = FieldCompanionColors.GoldInk,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Sem dados ao vivo · previsão salva na viagem",
                        style = TcType.label,
                        color = FieldCompanionColors.GoldInk,
                    )
                }
            }

            WeatherUi.Unavailable -> {
                Text(
                    text = "Clima",
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Neutral600,
                )
                Text(
                    text = "Sem previsão salva para este dia. O restante do dia continua disponível.",
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral700,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private fun range(minC: Double?, maxC: Double?): String? = when {
    minC != null && maxC != null -> "${minC.toInt()}° / ${maxC.toInt()}°"
    minC != null -> "mínima ${minC.toInt()}°"
    maxC != null -> "máxima ${maxC.toInt()}°"
    else -> null
}

@Composable
private fun OutfitCard(outfit: Outfit?, modifier: Modifier = Modifier) {
    TcCard(modifier = modifier) {
        if (outfit == null) {
            Text("O que vestir".uppercase(), style = TcType.eyebrow, color = FieldCompanionColors.Neutral600)
            Text(
                text = "Sem orientação salva para este dia.",
                style = TcType.meta,
                color = FieldCompanionColors.Neutral700,
                modifier = Modifier.padding(top = 4.dp),
            )
            return@TcCard
        }
        OutfitGroup("Vista", outfit.wear, topPadding = 0.dp)
        OutfitGroup("Leve", outfit.carry)
        OutfitGroup("Atenção", outfit.special)
    }
}

@Composable
private fun OutfitGroup(
    label: String,
    items: List<String>,
    topPadding: androidx.compose.ui.unit.Dp = 10.dp,
) {
    if (items.isEmpty()) return
    Text(
        text = label.uppercase(),
        style = TcType.eyebrow,
        color = FieldCompanionColors.Neutral600,
        modifier = Modifier.padding(top = topPadding),
    )
    Text(
        text = items.joinToString(", ") { it.replaceFirstChar(Char::lowercaseChar) },
        style = TcType.meta,
        color = FieldCompanionColors.Ink,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun TimelineCard(rows: List<TimelineRowUi>) {
    TcCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 18.dp,
            bottom = 4.dp,
        ),
    ) {
        rows.forEach { row -> TimelineRow(row) }
    }
}

/**
 * A timeline line: tabular hour, marker, title and one operational detail.
 * Past items lose opacity *and* colour, so the distinction never rests on
 * hue alone.
 */
@Composable
private fun TimelineRow(row: TimelineRowUi) {
    val markerColor = when {
        row.isCritical -> FieldCompanionColors.Oxblood
        row.state == TimelineState.Active -> FieldCompanionColors.Teal
        else -> FieldCompanionColors.Neutral400
    }
    val timeColor = when {
        row.isCritical -> FieldCompanionColors.Oxblood
        row.state == TimelineState.Active -> FieldCompanionColors.Teal
        else -> FieldCompanionColors.Ink
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(if (row.state == TimelineState.Past) 0.6f else 1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = row.item.startTime,
            style = TcType.clockSmall,
            color = timeColor,
            modifier = Modifier
                .width(58.dp)
                .padding(top = 2.dp),
        )

        Box(
            modifier = Modifier
                .width(18.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!row.isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(FieldCompanionColors.Neutral200),
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(18.dp)
                    .clip(TcPillShape)
                    .background(FieldCompanionColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(TcPillShape)
                        .background(markerColor),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = if (row.isLast) 64.dp else 78.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = row.item.title,
                    style = TcType.bodyStrong,
                    color = FieldCompanionColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                if (row.showsNowPill) {
                    TcChip(text = "Agora", tone = TcChipTone.Teal)
                }
                row.bookingStatus?.let { status -> TcChip(text = status.label, tone = status.tone()) }
            }
            row.item.detail?.let { detail ->
                Text(
                    text = detail,
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** S5 — a booking status keeps its own tone; criticality is layered separately. */
private fun BookingStatusUi.tone(): TcChipTone = when (this) {
    BookingStatusUi.Reserved, BookingStatusUi.Paid -> TcChipTone.Moss
    BookingStatusUi.Verify, BookingStatusUi.Buy -> TcChipTone.Gold
    BookingStatusUi.Included -> TcChipTone.Neutral
}

@Composable
private fun ShortcutRow(shortcut: ShortcutUi, onClick: () -> Unit) {
    when (shortcut.kind) {
        ShortcutUi.Kind.Document -> TcShortcutRow(
            icon = TcIcons.Ticket,
            label = shortcut.label,
            onClick = onClick,
            trailingNote = shortcut.trailingNote,
        )

        ShortcutUi.Kind.PlanB -> TcShortcutRow(
            icon = TcIcons.AltRoute,
            label = shortcut.label,
            onClick = onClick,
            iconTint = FieldCompanionColors.GoldInk,
            fill = FieldCompanionColors.GoldSoft,
            borderColor = FieldCompanionColors.GoldBorder,
            chevronTint = FieldCompanionColors.GoldInk,
        )

        ShortcutUi.Kind.Memory -> TcShortcutRow(
            icon = TcIcons.Mic,
            label = shortcut.label,
            onClick = onClick,
            iconTint = FieldCompanionColors.Oxblood,
        )
    }
}

private fun documentShortcutFor(critical: CriticalItemUi): ShortcutUi = ShortcutUi(
    id = critical.documentAction?.uri.orEmpty().substringAfter("document:"),
    label = critical.documentAction?.label ?: "Documento",
    kind = ShortcutUi.Kind.Document,
)
