package com.travelcompanion.app.domain.alerts

import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Which screen a critical alert opens, because a deadline belongs to a thing. */
sealed interface AlertTarget {
    /** Screen 15. */
    data class Transport(val transportId: String) : AlertTarget

    /** Screen 16. */
    data class Stay(val accommodationId: String) : AlertTarget

    /** Nothing owns it, so the day it falls on is where it opens. */
    data object Day : AlertTarget
}

/**
 * One deadline, resolved to the instant a phone should ring.
 *
 * [zone] is carried beside [at] on purpose: two zones that share an offset
 * today produce the same instant, so an instant alone cannot show that the
 * right rule was applied. The test asserts the zone.
 */
data class CriticalAlert(
    val criticalItemId: String,
    val title: String,
    val instruction: String,
    val at: Instant,
    val zone: ZoneId,
    val target: AlertTarget,
)

/**
 * The deadlines this trip should announce, in order, from [after] onwards.
 *
 * Pure on purpose: which items become alarms and at what instant is the whole
 * of this feature's risk, and none of it needs Android. `AlarmManager` is
 * handed a list that was already decided here — the same split
 * `domain/walk` has from GPS.
 *
 * Four rules, each of which the package can break:
 *
 * - **an item with no `actionByTime` is not a deadline.** It may still be
 *   worth reading on screen 02; it is not something to ring about;
 * - **an item with no instruction is not announced.** The notification's body
 *   is the packaged sentence and nothing else. Silence beats invented text;
 * - **one alert per item, at its earliest occurrence.** An accommodation's
 *   critical item is pulled into every day of the stay, so "check-in fecha às
 *   21:00" reads on three days in Kotor — but it is one deadline, on the night
 *   the traveller arrives;
 * - **nothing in the past.** Scheduling a deadline that has gone is noise at
 *   best and a wrong instruction at worst.
 */
fun criticalAlerts(content: TripContent, after: Instant): List<CriticalAlert> {
    val earliest = LinkedHashMap<String, CriticalAlert>()

    content.days.forEach { day ->
        val zone = runCatching { ZoneId.of(day.timeZone) }.getOrNull() ?: return@forEach
        val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@forEach
        val overrides = zoneOverrides(day)
        val owners = ownersOf(content, day)

        content.criticalItemsFor(day).forEach { item ->
            val alert = alertFor(item, date, overrides[item.id] ?: zone, owners[item.id])
                ?: return@forEach
            val held = earliest[item.id]
            if (held == null || alert.at < held.at) earliest[item.id] = alert
        }
    }

    return earliest.values
        .filter { it.at.isAfter(after) }
        .sortedBy { it.at }
}

/**
 * The zone a critical item's times are written in, when it is not the day's.
 *
 * A timeline item may declare its own — day 3 leaves Amsterdam, crosses Corfu
 * in Athens and lands in Ksamil in Tirane — and a critical item hangs off a
 * timeline item, so it inherits that declaration rather than the day's.
 */
private fun zoneOverrides(day: TripDay): Map<String, ZoneId> = buildMap {
    day.timeline.forEach { timelineItem ->
        val zone = timelineItem.timeZone?.let { id -> runCatching { ZoneId.of(id) }.getOrNull() }
            ?: return@forEach
        timelineItem.criticalItemIds.forEach { id -> put(id, zone) }
    }
}

/** Which entity on this day declared each critical item, when one did. */
private fun ownersOf(content: TripContent, day: TripDay): Map<String, AlertTarget> = buildMap {
    day.transportIds.forEach { id ->
        content.transport(id)?.criticalItems?.forEach { put(it.id, AlertTarget.Transport(id)) }
    }
    day.accommodationIds.forEach { id ->
        content.accommodation(id)?.criticalItems?.forEach { put(it.id, AlertTarget.Stay(id)) }
    }
}

private fun alertFor(
    item: CriticalItem,
    date: LocalDate,
    zone: ZoneId,
    target: AlertTarget?,
): CriticalAlert? {
    val time = item.actionByTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        ?: return null
    if (item.instruction.isBlank()) return null

    return CriticalAlert(
        criticalItemId = item.id,
        title = item.title,
        instruction = item.instruction,
        // The date is the day's, the time is the item's, and the zone is the
        // one the package wrote that time in. Only then does it become an
        // instant — the conversion this feature exists to get right.
        at = date.atTime(time).atZone(zone).toInstant(),
        zone = zone,
        target = target ?: AlertTarget.Day,
    )
}
