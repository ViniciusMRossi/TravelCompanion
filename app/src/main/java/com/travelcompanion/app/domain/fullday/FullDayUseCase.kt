package com.travelcompanion.app.domain.fullday

import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import com.travelcompanion.app.domain.today.CriticalItemUi
import com.travelcompanion.app.domain.today.ShortcutUi
import com.travelcompanion.app.domain.today.TimelineRowUi
import com.travelcompanion.app.domain.today.TodayUseCase
import java.time.LocalDate
import java.time.LocalTime

/** The three parts the approved screen reads a day in. */
enum class DayPeriod(val label: String) {
    Morning("Manhã"),
    Afternoon("Tarde"),
    Evening("Noite"),
}

/** One part of the day, with the timeline rows that fall inside it. */
data class DayPeriodSection(val period: DayPeriod, val rows: List<TimelineRowUi>)

/** The end-of-day transport card: two ends, the operator and the platform. */
data class DayTransportUi(
    val id: String,
    val originName: String,
    val originTime: String,
    val destinationName: String,
    val destinationTime: String,
    val operator: String?,
    val platform: String?,
)

/** The end-of-day stay card. */
data class DayStayUi(val id: String, val name: String, val checkIn: String?)

/** Screen 03 state. */
data class FullDayUiState(
    val date: LocalDate,
    val dateLabel: String,
    /** "Dia 9 de 21 · Sarajevo" */
    val dayLabel: String,
    val title: String,
    /** The day's critical item, summarised, above everything else. */
    val critical: CriticalItemUi?,
    val sections: List<DayPeriodSection>,
    val transports: List<DayTransportUi>,
    val stays: List<DayStayUi>,
    /** Documents of this day and its Plan B, as the footer draws them. */
    val shortcuts: List<ShortcutUi>,
    /** Null at the ends of the trip: there is no day 0 and no day 22. */
    val previousDate: LocalDate?,
    val nextDate: LocalDate?,
)

/**
 * Builds screen 03 — the same day as Today, read end to end.
 *
 * It is the same day, so it is the same model: the timeline rows, the critical
 * items and the day's shortcuts all come from [TodayUseCase] rather than from
 * a second reading of the package. What this adds is the part Today does not
 * have — the day split into morning, afternoon and evening, the end-of-day
 * cards, and the two arrows.
 */
class FullDayUseCase(private val content: TripContent) {

    private val today = TodayUseCase(content)
    private val days: List<TripDay> = content.days.sortedBy { it.date }

    operator fun invoke(
        date: LocalDate,
        time: LocalTime,
        /** The date the traveller is living, which [date] usually is not. */
        currentDate: LocalDate = date,
    ): FullDayUiState? {
        val day = content.dayFor(date) ?: return null
        val state = today(
            participantId = null,
            date = date,
            time = time,
            today = currentDate,
        ) ?: return null
        val index = days.indexOfFirst { it.id == day.id }

        return FullDayUiState(
            date = runCatching { LocalDate.parse(day.date) }.getOrDefault(date),
            dateLabel = state.dateLabel,
            dayLabel = listOfNotNull(state.dayLabel, state.cityName).joinToString(" · "),
            title = state.title,
            // "Resumido": the day may declare several, and the screen opens
            // with the one that is next to go wrong, not with all of them.
            critical = state.criticalItems.firstOrNull(),
            sections = sectionsOf(state.timeline),
            transports = day.transportIds.mapNotNull { id -> content.transport(id)?.let(::transportCard) },
            stays = day.accommodationIds.mapNotNull { id ->
                content.accommodation(id)?.let { DayStayUi(it.id, it.name, it.checkIn.from) }
            },
            // Today's own shortcuts stay on Today: "Gravar memória", and the
            // food row. The food row is left out for a sharper reason than
            // tidiness — this screen renders a *browsed* day, screen 20 always
            // opens on the day being lived, and a row that names one city
            // while opening another is precisely the confusion D089 names.
            shortcuts = state.shortcuts.filter {
                it.kind != ShortcutUi.Kind.Memory && it.kind != ShortcutUi.Kind.Food
            },
            previousDate = days.getOrNull(index - 1)?.let(::dateOf),
            nextDate = days.getOrNull(index + 1)?.let(::dateOf),
        )
    }

    /**
     * The periods, in order, with the empty ones dropped.
     *
     * A day that is all afternoon should not draw two empty headings.
     */
    private fun sectionsOf(rows: List<TimelineRowUi>): List<DayPeriodSection> =
        DayPeriod.entries
            .map { period -> DayPeriodSection(period, rows.filter { periodOf(it.item.startTime) == period }) }
            .filter { it.rows.isNotEmpty() }

    private fun transportCard(transport: com.travelcompanion.app.data.trip.Transport) = DayTransportUi(
        id = transport.id,
        originName = transport.origin.name,
        originTime = timeOf(transport.origin.dateTime),
        destinationName = transport.destination.name,
        destinationTime = timeOf(transport.destination.dateTime),
        operator = transport.operator,
        platform = transport.origin.platform,
    )

    private fun dateOf(day: TripDay): LocalDate? =
        runCatching { LocalDate.parse(day.date) }.getOrNull()
}

/**
 * Which part of the day a time falls in.
 *
 * Noon and six in the evening are the boundaries the approved screen reads
 * with; an unparseable time is treated as morning so it is never dropped.
 */
fun periodOf(startTime: String?): DayPeriod {
    val time = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return DayPeriod.Morning
    return when {
        time < LocalTime.NOON -> DayPeriod.Morning
        time < LocalTime.of(18, 0) -> DayPeriod.Afternoon
        else -> DayPeriod.Evening
    }
}

/** "2026-09-13T19:30:00" as "19:30". */
private fun timeOf(dateTime: String): String =
    runCatching { java.time.LocalDateTime.parse(dateTime).toLocalTime().toString() }
        .getOrDefault(dateTime.substringAfter('T').take(5))
