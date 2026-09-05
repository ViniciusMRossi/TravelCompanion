package com.travelcompanion.app.feature.stay

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.operations.ActionWindow
import com.travelcompanion.app.domain.operations.PhoneUi
import com.travelcompanion.app.domain.operations.actionWindow
import com.travelcompanion.app.domain.operations.phone
import com.travelcompanion.app.domain.today.BookingStatusUi
import java.time.LocalTime

/** Screen 16 state. */
data class StayUiState(
    val name: String,
    val heroAssetPath: String?,
    val heroCaption: String,
    val status: BookingStatusUi?,
    val checkIn: String,
    val checkOut: String,
    val critical: CriticalItem?,
    val window: ActionWindow,
    val hostPhone: PhoneUi?,
    val actions: List<ActionLink>,
    val voucherId: String?,
    val instructions: String?,
)

/**
 * Builds screen 16.
 *
 * The hero draws the striped placeholder while no photography is packaged and
 * the name uses the serif fallback while Fraunces is absent — both are Phase 1
 * items still open (D005), not divergences, and the screen is drawn as the
 * sheet asks with those two substitutes showing.
 */
fun buildStayState(
    content: TripContent,
    stayId: String?,
    now: LocalTime,
): StayUiState? {
    val stay = content.trip.accommodations.firstOrNull { it.id == stayId } ?: return null
    val critical = stay.criticalItems.firstOrNull()

    return StayUiState(
        name = stay.name,
        heroAssetPath = content.assets.packagedPathIfPresent(stay.heroAssetId),
        heroCaption = "foto — ${stay.name}",
        status = BookingStatusUi.from(stay.bookingStatus),
        checkIn = window(stay.checkIn.from, stay.checkIn.until),
        checkOut = window(stay.checkOut.from, stay.checkOut.until),
        critical = critical,
        window = critical?.let { actionWindow(it, now) } ?: ActionWindow.Ahead,
        // The host's number is trip data and is withheld while the package
        // carries examples, exactly like the insurer's on screen 17 (D064).
        hostPhone = stay.contactPhone?.let { number ->
            phone(
                label = "Ligar para o anfitrião",
                number = number,
                detail = stay.contactName,
                isMockContent = content.trip.metadata.isMockContent,
            )
        },
        actions = stay.actions.filterNot { it.kind == "phone" },
        // fallback: the schema declares no primary document for a stay and the
        // screen has room for one voucher, so this is the order the package was
        // written in. A stay carrying two documents shows whichever came first.
        voucherId = stay.documentIds.firstOrNull(),
        instructions = stay.instructions,
    )
}

/** "a partir das 14:00 · até 23:00", from whichever ends the package gives. */
private fun window(from: String?, until: String?): String = when {
    from != null && until != null -> "$from — $until"
    from != null -> "a partir das $from"
    until != null -> "até $until"
    else -> "—"
}
