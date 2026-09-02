package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.Outfit
import com.travelcompanion.app.data.trip.Participant
import com.travelcompanion.app.data.trip.TimelineItem

/** S3 — timeline item states. Never distinguished by hue alone. */
enum class TimelineState { Past, Active, Upcoming }

/**
 * S5 — booking status.
 *
 * Criticality is an independent dimension layered over this, never a
 * replacement for it: an item can be Reserved *and* critical.
 */
enum class BookingStatusUi(val label: String) {
    Reserved("Reservado"),
    Paid("Pago"),
    Verify("Verificar"),
    Buy("Comprar"),
    Included("Incluído"),
    ;

    companion object {
        fun from(raw: String?): BookingStatusUi? = when (raw) {
            "reserved" -> Reserved
            "paid" -> Paid
            "verify" -> Verify
            "buy" -> Buy
            "included" -> Included
            else -> null
        }
    }
}

data class TimelineRowUi(
    val item: TimelineItem,
    val state: TimelineState,
    val isCritical: Boolean,
    val bookingStatus: BookingStatusUi?,
    val isLast: Boolean,
) {
    val showsNowPill: Boolean get() = state == TimelineState.Active
}

/**
 * The ink card at the top of Today.
 *
 * [isActive] false means nothing has started yet and this is the next
 * commitment rather than the current one; the eyebrow says which.
 */
data class NowBlockUi(
    val time: String,
    val title: String,
    val isActive: Boolean,
    val detail: String?,
    val nextLine: String?,
    val audioLine: String?,
    val attractionId: String?,
    val mapsAction: ActionLink?,
)

data class CriticalItemUi(
    val item: CriticalItem,
    val documentAction: ActionLink?,
    val mapsAction: ActionLink?,
) {
    /** The scheduled time. Kept visually separate from [item].instruction. */
    val nominalTime: String? get() = item.nominalTime
}

/**
 * Weather never blocks Today.
 *
 * V1 has no live provider yet (Phase 7), so the only reachable states are the
 * packaged trip fallback and "no data at all". Freshness is always stated
 * rather than implied.
 */
sealed interface WeatherUi {
    data class FallbackFromTrip(
        val summary: String?,
        val minC: Double?,
        val maxC: Double?,
        val rainNote: String?,
        val windNote: String?,
    ) : WeatherUi

    data object Unavailable : WeatherUi
}

data class ShortcutUi(
    val id: String,
    val label: String,
    val kind: Kind,
    val trailingNote: String? = null,
) {
    enum class Kind { Document, PlanB, Memory }
}

data class TodayUiState(
    val participant: Participant?,
    val cityName: String?,
    val countryName: String?,
    val dayLabel: String,
    val dateLabel: String,
    val title: String,
    val now: NowBlockUi?,
    val criticalItems: List<CriticalItemUi>,
    val weather: WeatherUi,
    val outfit: Outfit?,
    val timeline: List<TimelineRowUi>,
    val shortcuts: List<ShortcutUi>,
    val offlineNote: String,
)
