package com.travelcompanion.app.feature.document

import com.travelcompanion.app.data.trip.DocumentAccess
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDocument
import com.travelcompanion.app.data.trip.documentAccess
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Why a QR is or is not on screen.
 *
 * Three of these are content answers, knowable before anyone taps, which is
 * what lets screen 14 say them instead of showing an empty square (D021).
 */
sealed interface QrState {

    /** A code the traveller can hold up, however it got here. */
    sealed interface Ready : QrState

    /** Encoded here, from the document's own text. */
    data class Generated(val text: String) : Ready

    /**
     * Shipped as an image in the package.
     *
     * Kept apart from [Generated] on purpose: the two are both "there is a
     * code", and treating them as one string would have encoded the asset's
     * *path* into a QR and produced a scannable square holding
     * `trip/documents/...`.
     */
    data class Embedded(val assetPath: String) : Ready

    /** The document declares no code. Not a failure; most documents have none. */
    data object None : QrState

    /**
     * The package's ticket data is mock, so no code is generated.
     *
     * Brief §18: a QR must come from the actual document or actual encoded
     * ticket data, and never from a placeholder. `MOCK-ABC123` encoded into a
     * scannable square is a placeholder that looks real at a counter, which is
     * worse than none at all (D061).
     */
    data object MockContent : QrState

    /** `embedded` mode, but the image is not in this build. */
    data object AssetMissing : QrState
}

/** Screen 14, ficha mode. */
data class DocumentUiState(
    val title: String,
    val subtitle: String?,
    val journey: JourneyUi?,
    val passengers: String?,
    val locator: String?,
    val price: String?,
    val qr: QrState,
    /** The redundancy line: the code is not the only way in. */
    val locatorNote: String?,
    val isPackaged: Boolean,
    val notPackagedNote: String?,
    /**
     * The packaged file, when there is one: where it sits in the assets, what
     * it is, and what a viewer should call it. Null is the whole of
     * `NotPackaged` — a control that opens nothing is worse than no control
     * (D091).
     */
    val file: PackagedFileUi?,
)

/** A document's own file, ready to be handed to whatever app reads it. */
data class PackagedFileUi(
    val assetPath: String,
    val mimeType: String?,
    /** What the viewer's title bar reads. Never the asset's build path. */
    val displayName: String,
)

/** The two ends of a leg, drawn as a ticket draws them. */
data class JourneyUi(
    val originName: String,
    val originTime: String,
    val destinationName: String,
    val destinationTime: String,
    val platform: String?,
    val operator: String?,
)

/**
 * Builds screen 14 from the package.
 *
 * The ticket is drawn from trip data rather than from the PDF: the approved
 * sheet draws a ficha — times, passengers, platform, locator, price — and that
 * is data the package already carries, which is also what makes it readable
 * when the file itself is not in this build.
 */
fun buildDocumentState(
    content: TripContent,
    documentId: String?,
): DocumentUiState? {
    val access = documentAccess(content, documentId) ?: return null
    val document = access.document
    val transport = content.trip.transports.firstOrNull { documentId in it.documentIds }

    val passengers = document.participantIds
        .mapNotNull { content.participant(it)?.name }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

    val locator = document.locator ?: transport?.bookingReference

    return DocumentUiState(
        title = document.title,
        subtitle = document.subtitle,
        journey = transport?.let {
            JourneyUi(
                originName = it.origin.name,
                originTime = clock(it.origin.dateTime),
                destinationName = it.destination.name,
                destinationTime = clock(it.destination.dateTime),
                platform = it.origin.platform,
                operator = it.operator,
            )
        },
        passengers = passengers,
        locator = locator,
        price = transport?.price,
        qr = qrState(content, document),
        // The approved sheet's redundancy: the driver takes the locator too,
        // which is the sentence that matters when a screen will not scan.
        locatorNote = locator?.let {
            "O motorista também aceita o localizador. Mostre o código ou leia o número."
        },
        isPackaged = access is DocumentAccess.Packaged,
        notPackagedNote = (access as? DocumentAccess.NotPackaged)?.let {
            "O arquivo deste documento não está neste aparelho. " +
                "Os dados abaixo estão salvos e funcionam offline."
        },
        file = (access as? DocumentAccess.Packaged)?.let {
            PackagedFileUi(
                assetPath = it.assetPath,
                mimeType = it.mimeType,
                displayName = fileNameOf(document.title, it.assetPath),
            )
        },
    )
}

/**
 * Whether there is a code, and where it would come from.
 *
 * `embedded` resolves to a packaged image and needs nothing generated.
 * `generated-from-text` is encoded from the document's own text — but only
 * when the package is carrying real data. A trip marked `isMockContent` has
 * `MOCK-ABC123` where a locator belongs, and §18 says a placeholder QR is
 * never shown (D061).
 */
private fun qrState(content: TripContent, document: TripDocument): QrState {
    val qr = document.qr ?: return QrState.None
    val declared = when (qr.mode) {
        "embedded" -> {
            val path = content.assets.packagedPathIfPresent(qr.embeddedAssetId)
            if (path != null) QrState.Embedded(path) else QrState.AssetMissing
        }
        "generated-from-text" -> {
            val text = qr.text?.takeIf { it.isNotBlank() }
            if (text != null) QrState.Generated(text) else QrState.None
        }
        else -> QrState.None
    }

    // Mock content withholds a code however the code would have arrived.
    //
    // The gate first covered only `generated-from-text`, which was the wrong
    // half to guard: §18 says never use a *visual* placeholder QR, and a
    // packaged image is the visual case — if anything more convincing at a
    // counter, because it was shipped as a picture (D061, D063).
    //
    // It applies to the branches that would have produced a code and to no
    // others: a document declaring `mode: none` has no code in any package,
    // and telling its holder that one arrives with the real ticket would be a
    // different untruth.
    return if (declared is QrState.Ready && content.trip.metadata.isMockContent) {
        QrState.MockContent
    } else {
        declared
    }
}

/** "19:30" from the package's local date-time, which carries no offset. */
private fun clock(dateTime: String): String =
    runCatching { LocalDateTime.parse(dateTime).format(CLOCK) }.getOrDefault(dateTime)

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The document's own title as a file name, keeping the packaged extension.
 *
 * The same substitution `MemoryFileProvider` makes for a memory, and for the
 * same reason: what the traveller sees named should be the thing they asked
 * for, not the build's path for it (D082).
 */
private fun fileNameOf(title: String, assetPath: String): String {
    val extension = assetPath.substringAfterLast('.', "").takeIf { it.isNotBlank() }
    val stem = title.map { if (it.isLetterOrDigit() || it in " -_") it else ' ' }
        .joinToString("")
        .trim()
        .ifBlank { "documento" }
    return if (extension == null) stem else "$stem.$extension"
}
