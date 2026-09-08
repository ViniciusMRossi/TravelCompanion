package com.travelcompanion.app.feature.transport

import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.PlanB
import com.travelcompanion.app.data.trip.Transport
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.operations.ActionWindow
import com.travelcompanion.app.domain.operations.PhoneUi
import com.travelcompanion.app.domain.operations.actionWindow
import com.travelcompanion.app.domain.operations.phone
import java.time.LocalTime

/** One end of the leg, as the approved vertical journey draws it. */
data class LegEndUi(
    val time: String,
    val stationName: String,
    val platform: String?,
)

/** Screen 15 state. */
data class TransportUiState(
    val title: String,
    /** The eyebrow above the journey: mode, operator, and the service number. */
    val serviceLine: String,
    val critical: CriticalItem?,
    val window: ActionWindow,
    val criticalActions: List<ActionLink>,
    val origin: LegEndUi,
    val destination: LegEndUi,
    val durationLabel: String?,
    val peopleLabel: String?,
    val price: String?,
    val operatorPhone: PhoneUi?,
    val actions: List<ActionLink>,
    val documentId: String?,
    val planB: PlanB?,
)

/**
 * Builds screen 15.
 *
 * The critical item sits at the top with its instruction apart from the
 * departure time — they are two different facts and the approved screen never
 * merges them. Everything else is the leg: two ends, a footer of metadata, and
 * the actions the package itself declares.
 */
fun buildTransportState(
    content: TripContent,
    transportId: String?,
    now: LocalTime,
): TransportUiState? {
    val transport = content.trip.transports.firstOrNull { it.id == transportId } ?: return null
    val critical = transport.criticalItems.firstOrNull()
    // fallback: the schema declares no primary document for a leg and the
    // screen has room for one ticket, so this is the order the package was
    // written in. A leg carrying two documents shows whichever came first.
    val document = transport.documentIds.firstOrNull()

    val people = content.trip.documents
        .firstOrNull { it.id == document }
        ?.participantIds
        ?.mapNotNull { content.participant(it)?.name }
        ?.takeIf { it.isNotEmpty() }

    return TransportUiState(
        title = "${transport.origin.name} → ${transport.destination.name}",
        serviceLine = serviceLine(transport),
        critical = critical,
        window = critical?.let { actionWindow(it, now) } ?: ActionWindow.Ahead,
        criticalActions = critical?.actionLinks.orEmpty(),
        origin = LegEndUi(
            time = clock(transport.origin.dateTime),
            stationName = transport.origin.name,
            platform = transport.origin.platform,
        ),
        destination = LegEndUi(
            time = clock(transport.destination.dateTime),
            stationName = transport.destination.name,
            platform = transport.destination.platform,
        ),
        durationLabel = duration(transport.origin.dateTime, transport.destination.dateTime),
        peopleLabel = people?.let { names ->
            if (names.size == 1) names.single() else "${names.size} pessoas · ${names.joinToString(" · ")}"
        },
        price = transport.price,
        // The operator's line is trip data like any other, so it is subject to
        // the same gate as the rest (D064).
        operatorPhone = transport.actions.firstOrNull { it.kind == "phone" }?.let { action ->
            phone(
                label = action.label,
                number = action.uri.removePrefix("tel:"),
                isMockContent = content.trip.metadata.isMockContent,
            )
        },
        actions = transport.actions.filterNot { it.kind == "phone" },
        documentId = document,
        planB = content.planB(transport.planBId),
    )
}

/**
 * How the approved artboard names a leg: an eyebrow above the vertical
 * journey, reading "Ônibus · Centrotrans" — mode, then operator.
 *
 * The app drew no such row, and `serviceNumber` was parsed and read nowhere,
 * so the five flights that declare `LA 8078`, `EJU7913`, `OU 661`, `OU 450`
 * and `LA 8079` never showed one. The number is the third term here: it is
 * what the airport board lists and what the counter asks for, and at 04:45 on
 * 30/09 "Croatia Airlines" alone is not enough to find the queue. The artboard
 * shows two terms because its bus has no number; the third is a deliberate
 * addition, approved (D177).
 *
 * Whatever is missing simply drops out, so a leg with neither operator nor
 * number still gets its mode and the row never appears empty.
 */
private fun serviceLine(transport: Transport): String =
    listOfNotNull(modeLabel(transport.type), transport.operator, transport.serviceNumber)
        .joinToString(" · ")

/** The schema's nine modes, in the plate's own register. */
private fun modeLabel(type: String): String = when (type) {
    "flight" -> "Voo"
    "bus" -> "Ônibus"
    "ferry" -> "Ferry"
    "train" -> "Trem"
    "transfer" -> "Transfer"
    "taxi" -> "Táxi"
    "urban" -> "Transporte urbano"
    "walk" -> "A pé"
    else -> "Transporte"
}

private fun clock(dateTime: String): String =
    runCatching { java.time.LocalDateTime.parse(dateTime).toLocalTime().toString() }
        .getOrDefault(dateTime)

/** "2h30" — approximate on purpose; the package declares no travel time. */
private fun duration(from: String, to: String): String? = runCatching {
    val start = java.time.LocalDateTime.parse(from)
    val end = java.time.LocalDateTime.parse(to)
    val minutes = java.time.Duration.between(start, end).toMinutes()
    if (minutes <= 0) return@runCatching null
    val hours = minutes / 60
    val rest = minutes % 60
    when {
        hours > 0 && rest > 0 -> "${hours}h${"%02d".format(rest)}"
        hours > 0 -> "${hours}h"
        else -> "$rest min"
    }
}.getOrNull()
