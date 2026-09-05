package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.TimelineItem
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import com.travelcompanion.app.data.trip.WeatherFallback
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Builds the Today screen state from packaged content plus the clock.
 *
 * Everything Today needs to decide — which day, what is happening now, what
 * cannot go wrong, what is already behind — is computed here so the composable
 * only lays out an answer it was given.
 */
class TodayUseCase(
    private val content: TripContent,
) {
    private val locale: Locale = localeOf(content.info.locale)

    operator fun invoke(
        participantId: String?,
        date: LocalDate,
        time: LocalTime,
        /**
         * The date the traveller is actually living, which is only the same as
         * [date] on screen 02. Screen 03 renders a browsed day and must not
         * claim anything on it is under way (D089).
         */
        today: LocalDate = date,
    ): TodayUiState? {
        val day = content.dayFor(date) ?: return null
        val timeline = day.timeline.sortedBy { it.startTime }
        val activeIndex = activeIndexOf(timeline, today, day, time)
        val city = content.city(day.baseCityId ?: day.cityIds.firstOrNull())

        return TodayUiState(
            participant = content.participant(participantId),
            cityName = city?.name,
            countryName = city?.countryName,
            dayLabel = "Dia ${day.dayNumber} de ${content.totalDays}",
            dateLabel = formatDate(day.date),
            title = day.title ?: city?.name ?: content.info.title,
            now = nowBlock(day, timeline, activeIndex, focusIndexOf(timeline, today, day, time, activeIndex)),
            criticalItems = criticalItems(day),
            weather = weather(day.weatherFallback),
            outfit = day.outfit,
            timeline = timelineRows(day, timeline, activeIndex, date, time),
            shortcuts = shortcuts(day),
            offlineNote = OFFLINE_NOTE,
        )
    }

    /**
     * Index of the item currently under way: the last one that has started.
     *
     * Days other than the real current date have no "now" — reading tomorrow
     * must not claim something is happening. The comparison is against the
     * date the traveller is living, never against the date being rendered:
     * those are the same thing on screen 02 and are not on screen 03, where
     * comparing a browsed day to itself always agrees and the guard would
     * stop guarding (D089).
     */
    private fun activeIndexOf(
        timeline: List<TimelineItem>,
        today: LocalDate,
        day: TripDay,
        time: LocalTime,
    ): Int? {
        if (day.date != today.toString()) return null
        val index = timeline.indexOfLast { parseTime(it.startTime)?.let { start -> !start.isAfter(time) } == true }
        if (index < 0) return null
        val item = timeline[index]
        val end = item.endTime?.let(::parseTime)
        val overtakenByNext = timeline.getOrNull(index + 1)
            ?.let { parseTime(it.startTime) }
            ?.let { !it.isAfter(time) } == true
        if (overtakenByNext) return null
        if (end != null && time.isAfter(end) && timeline.size > index + 1) return null
        return index
    }

    /**
     * The item the ink card speaks about: what is under way, or failing that
     * the next thing that has not started yet.
     */
    private fun focusIndexOf(
        timeline: List<TimelineItem>,
        today: LocalDate,
        day: TripDay,
        time: LocalTime,
        activeIndex: Int?,
    ): Int? {
        if (activeIndex != null) return activeIndex
        if (timeline.isEmpty()) return null
        if (day.date != today.toString()) return 0
        val upcoming = timeline.indexOfFirst {
            parseTime(it.startTime)?.isAfter(time) == true
        }
        return if (upcoming >= 0) upcoming else timeline.lastIndex
    }

    private fun nowBlock(
        day: TripDay,
        timeline: List<TimelineItem>,
        activeIndex: Int?,
        focusIndex: Int?,
    ): NowBlockUi? {
        val index = focusIndex ?: return null
        val item = timeline.getOrNull(index) ?: return null
        val next = timeline.getOrNull(index + 1)
        val attraction = content.attractionFor(item)
        val walk = content.walkFor(item)

        val audioGuide = content.audioGuide(
            attraction?.audioGuideId
                ?: content.city(day.baseCityId)?.audioGuideId,
        )
        val audioLine = audioGuide?.let {
            val saved = if (content.assets.isAvailableOffline(it.audioAssetId)) {
                "salvo no aparelho"
            } else {
                "ainda não salvo neste aparelho"
            }
            "${it.title} · ${it.durationMinutes} min · $saved"
        }

        return NowBlockUi(
            time = item.startTime,
            title = attraction?.name ?: walk?.title ?: item.title,
            isActive = activeIndex != null,
            detail = item.detail ?: attraction?.subtitle ?: walk?.subtitle,
            nextLine = next?.let { "${it.title} às ${it.startTime}." },
            audioLine = audioLine,
            attractionId = attraction?.id,
            mapsAction = attraction?.actions?.firstOrNull { it.kind == "maps" },
        )
    }

    private fun criticalItems(day: TripDay): List<CriticalItemUi> =
        content.criticalItemsFor(day).map { critical ->
            CriticalItemUi(
                item = critical,
                documentAction = critical.actionLinks.firstOrNull { it.kind == "document" },
                mapsAction = critical.actionLinks.firstOrNull { it.kind == "maps" },
            )
        }

    private fun weather(fallback: WeatherFallback?): WeatherUi =
        if (fallback == null) {
            WeatherUi.Unavailable
        } else {
            WeatherUi.FallbackFromTrip(
                summary = fallback.summary,
                minC = fallback.minC,
                maxC = fallback.maxC,
                rainNote = fallback.rainNote,
                windNote = fallback.windNote,
            )
        }

    private fun timelineRows(
        day: TripDay,
        timeline: List<TimelineItem>,
        activeIndex: Int?,
        date: LocalDate,
        time: LocalTime,
    ): List<TimelineRowUi> {
        val criticalIds = content.criticalItemsFor(day).map { it.id }.toSet()
        val isToday = day.date == date.toString()
        val dayIsBehind = isToday.not() && runCatching { LocalDate.parse(day.date) < date }.getOrDefault(false)
        return timeline.mapIndexed { index, item ->
            TimelineRowUi(
                item = item,
                state = when {
                    index == activeIndex -> TimelineState.Active
                    dayIsBehind -> TimelineState.Past
                    !isToday -> TimelineState.Upcoming
                    parseTime(item.startTime)?.isAfter(time) == false -> TimelineState.Past
                    else -> TimelineState.Upcoming
                },
                isCritical = item.criticalItemIds.any(criticalIds::contains),
                bookingStatus = BookingStatusUi.from(item.bookingStatus),
                isLast = index == timeline.lastIndex,
            )
        }
    }

    private fun shortcuts(day: TripDay): List<ShortcutUi> = buildList {
        day.documentIds.mapNotNull(content::document).forEach { document ->
            // Declaring availableOffline is a content promise; the badge is only
            // earned once the file is actually in this build. Saying "Offline"
            // over a missing asset is the one lie that hurts at a bus station.
            val resolvesLocally = document.availableOffline &&
                content.assets.isAvailableOffline(document.assetId)
            add(
                ShortcutUi(
                    id = document.id,
                    label = document.title,
                    kind = ShortcutUi.Kind.Document,
                    trailingNote = if (resolvesLocally) "Offline" else null,
                )
            )
        }
        content.planB(day.planBId)?.let { planB ->
            add(
                ShortcutUi(
                    id = planB.id,
                    label = "Plano B · ${planB.scenario.replaceFirstChar { it.lowercase(locale) }}",
                    kind = ShortcutUi.Kind.PlanB,
                )
            )
        }
        add(ShortcutUi(id = "memory", label = "Gravar memória", kind = ShortcutUi.Kind.Memory))
    }

    /** "Quinta, 17 de setembro" */
    private fun formatDate(isoDate: String): String {
        val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
        val weekday = date.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .substringBefore("-")
            .replaceFirstChar { it.titlecase(locale) }
        val month = date.month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
        return "$weekday, ${date.dayOfMonth} de $month"
    }

    private fun parseTime(raw: String?): LocalTime? =
        raw?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    private companion object {
        const val OFFLINE_NOTE = "Offline · conteúdo disponível"

        fun localeOf(tag: String): Locale = runCatching {
            Locale.forLanguageTag(tag).takeIf { it.language.isNotEmpty() }
        }.getOrNull() ?: Locale.forLanguageTag("pt-BR")
    }
}

/** Maps action of a critical item, if the content package provides one. */
val CriticalItemUi.hasActions: Boolean
    get() = documentAction != null || mapsAction != null

/** Convenience for screens that only need the label of an action. */
val ActionLink.isOffline: Boolean
    get() = !requiresInternet
