package com.travelcompanion.app.domain.wallet

import com.travelcompanion.app.data.trip.DocumentAccess
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDocument
import com.travelcompanion.app.data.trip.allDocumentsResolve
import com.travelcompanion.app.data.trip.documentAccess
import com.travelcompanion.app.domain.today.BookingStatusUi
import java.time.LocalDate

/** The three groups screen 13 shows, in the order it shows them. */
enum class WalletGroup(val title: String) {
    Today("Hoje"),
    Transport("Transporte da viagem"),
    Papers("Seguro e documentos"),
}

/** One row of the wallet: a document and what the trip knows about it. */
data class WalletRow(
    val documentId: String,
    val title: String,
    /** The operational line under the name — what it is for, not what it is. */
    val detail: String?,
    val status: BookingStatusUi?,
    val type: String,
    /** False when the package promises offline and the file is not in this build. */
    val resolvesOffline: Boolean,
)

data class WalletSection(val group: WalletGroup, val rows: List<WalletRow>)

data class WalletUiState(
    val sections: List<WalletSection>,
    val totalCount: Int,
    /**
     * Whether the header may say "Tudo offline".
     *
     * False the moment one document declares offline without its file: the
     * pill is a claim about the whole wallet and cannot be truer than its
     * weakest row (D013, D060).
     */
    val allOffline: Boolean,
)

/**
 * Screen 13, decided without Compose.
 *
 * Grouping is a question about content and dates, not about drawing: which
 * documents belong to the day the traveller is on, which belong to getting
 * from place to place, and which are the papers that sit in the background
 * for the whole trip. So it is a pure function, testable with a fixed date
 * the way `domain/walk` is testable without GPS.
 *
 * "Today" comes from [TripContent.dayFor] — the current-day logic Phase 1
 * already has, including how it behaves before the trip starts. There is no
 * second one.
 */
fun buildWalletState(content: TripContent, date: LocalDate): WalletUiState {
    val today = content.dayFor(date)?.date

    val transportDocumentIds = content.trip.transports.flatMap { it.documentIds }.toSet()
    val statuses = bookingStatuses(content)

    val rows = content.trip.documents.map { document ->
        WalletRow(
            documentId = document.id,
            title = document.title,
            detail = document.subtitle ?: validityLine(document),
            status = statuses[document.id],
            type = document.type,
            resolvesOffline = documentAccess(content, document.id) is DocumentAccess.Packaged,
        )
    }

    val grouped = content.trip.documents.associateBy(TripDocument::id).let { byId ->
        rows.groupBy { row ->
            val document = byId.getValue(row.documentId)
            when {
                today != null && document.coversDay(today) -> WalletGroup.Today
                row.documentId in transportDocumentIds -> WalletGroup.Transport
                else -> WalletGroup.Papers
            }
        }
    }

    return WalletUiState(
        // Empty groups are not drawn: the approved screen is a list of what
        // there is, not a form with three headings.
        sections = WalletGroup.entries.mapNotNull { group ->
            grouped[group]?.takeIf { it.isNotEmpty() }?.let { WalletSection(group, it) }
        },
        totalCount = rows.size,
        allOffline = rows.isNotEmpty() && allDocumentsResolve(content),
    )
}

/**
 * A document's booking status, from whatever the trip says owns it.
 *
 * Documents carry no status of their own — the transport or the stay that
 * references them does, and that is where the approved row's "Pago" or
 * "Verificar" comes from rather than from a constant on the screen.
 */
private fun bookingStatuses(content: TripContent): Map<String, BookingStatusUi> = buildMap {
    content.trip.transports.forEach { transport ->
        val status = BookingStatusUi.from(transport.bookingStatus) ?: return@forEach
        transport.documentIds.forEach { put(it, status) }
    }
    content.trip.accommodations.forEach { stay ->
        val status = BookingStatusUi.from(stay.bookingStatus) ?: return@forEach
        stay.documentIds.forEach { put(it, status) }
    }
}

/** True when [isoDate] falls inside the document's declared validity. */
private fun TripDocument.coversDay(isoDate: String): Boolean {
    val from = validFrom
    val until = validUntil
    if (from == null && until == null) return false
    if (from != null && isoDate < from) return false
    if (until != null && isoDate > until) return false
    return true
}

/** "Válido em 21/09" or a range, when the document has no subtitle of its own. */
private fun validityLine(document: TripDocument): String? {
    val from = document.validFrom?.let(::dayMonth)
    val until = document.validUntil?.let(::dayMonth)
    return when {
        from != null && until != null && from != until -> "Válido de $from a $until"
        from != null -> "Válido em $from"
        until != null -> "Válido até $until"
        else -> null
    }
}

private fun dayMonth(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate) }
        .map { "%02d/%02d".format(it.dayOfMonth, it.monthValue) }
        .getOrDefault(isoDate)
