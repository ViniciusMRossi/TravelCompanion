package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.today.CriticalItemUi
import com.travelcompanion.app.domain.today.TimelineRowUi
import com.travelcompanion.app.domain.today.TimelineState
import com.travelcompanion.app.domain.today.TodayUseCase
import com.travelcompanion.app.domain.walk.WalkModeState
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** One of the four summary cards. */
data class WalkSummaryCardUi(val label: String, val value: String)

/** Screen 11 state. */
data class WalkFinishedUiState(
    val title: String,
    /** Where and when it ended, in one sentence. */
    val endedLine: String,
    val cards: List<WalkSummaryCardUi>,
    /** The commitments still ahead today. */
    val remaining: List<TimelineRowUi>,
    /** The day's critical item, still in oxblood with its instruction. */
    val critical: CriticalItemUi?,
)

/**
 * Builds screen 11 — the walk closed and the traveller handed back to the day.
 *
 * Every number on it comes from something already tracked. The duration is the
 * walk's own clock; the distance is the route's declared length, because
 * nothing here measures how far anybody actually walked and inventing an
 * odometer would be a second subsystem to keep true; the stories are the ones
 * whose audio really started, which the controller records separately from
 * merely having been triggered (§20); and who listened is this traveller plus
 * whatever the app already knows about the group — never a fresh question,
 * because asking one would join the group and D039/D071 keep that to screen 09
 * (D079).
 */
fun buildWalkFinishedState(
    content: TripContent,
    walkState: WalkModeState,
    localParticipantId: String?,
    knownGroup: List<GroupParticipant>,
    date: LocalDate,
    time: LocalTime,
): WalkFinishedUiState? {
    val walk = content.walk(walkState.walkId) ?: return null
    val today = TodayUseCase(content)(localParticipantId, date, time)

    val criticalId = today?.criticalItems?.firstOrNull()?.item?.id

    val listeners = buildList {
        content.participant(localParticipantId)?.name?.let(::add)
        knownGroup
            .map { it.id }
            .filter { it != localParticipantId }
            .mapNotNull { content.participant(it)?.name }
            .forEach(::add)
    }

    return WalkFinishedUiState(
        title = walk.title,
        endedLine = endedLine(content, walkState, walk.cityId),
        cards = listOf(
            WalkSummaryCardUi("Duração", durationLabel(walkState) ?: "${walk.durationMinutes} min"),
            WalkSummaryCardUi("Distância", distanceLabel(walk.distanceMeters)),
            WalkSummaryCardUi("Histórias ouvidas", "${walkState.playedStoryIds.size} de ${walkState.stops.size}"),
            WalkSummaryCardUi("Quem ouviu", listeners.joinToString(" · ").ifEmpty { "Você" }),
        ),
        // The two next commitments, which is what the sheet asks for: the rest
        // of the day belongs to screen 02. The critical item is drawn above
        // them in oxblood, so the row that *is* that item is not listed twice —
        // on a day whose next commitment is the critical one, it would
        // otherwise appear as both.
        remaining = today?.timeline.orEmpty()
            .filter { it.state == TimelineState.Upcoming }
            .filterNot { row -> criticalId != null && criticalId in row.item.criticalItemIds }
            .take(2),
        critical = today?.criticalItems?.firstOrNull(),
    )
}

/** "Terminou em Sarajevo, às 16:20." */
private fun endedLine(content: TripContent, walkState: WalkModeState, cityId: String): String {
    val city = content.city(cityId)?.name
    val at = walkState.completedAtEpochMs?.let(::clockOf)
    return listOfNotNull(
        city?.let { "Terminou em $it" } ?: "Passeio terminado",
        at?.let { "às $it" },
    ).joinToString(", ") + "."
}

private fun clockOf(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .let { String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute) }

/** The walk's own clock, when it has one. */
private fun durationLabel(walkState: WalkModeState): String? {
    val start = walkState.startedAtEpochMs ?: return null
    val end = walkState.completedAtEpochMs ?: return null
    val minutes = ((end - start) / 60_000L).toInt().coerceAtLeast(0)
    return "$minutes min"
}

/** "1,8 km" or "800 m" — the route's declared length. */
private fun distanceLabel(meters: Int): String =
    if (meters >= 1_000) {
        String.format(Locale.forLanguageTag("pt-BR"), "%.1f km", meters / 1_000.0)
    } else {
        "$meters m"
    }
