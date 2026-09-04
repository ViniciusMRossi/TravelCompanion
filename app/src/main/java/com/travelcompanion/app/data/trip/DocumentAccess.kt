package com.travelcompanion.app.data.trip

/**
 * Whether a document can actually be opened on this phone.
 *
 * The same shape [com.travelcompanion.app.service.playback.AudioGuideRequest]
 * has, and for the same reason (D021): availability is knowable from content
 * before anyone taps, so the screen says it up front instead of failing
 * afterwards. `availableOffline` is a promise the package makes; whether the
 * binary is in this build is a separate fact, and D013 already decided that
 * the promise without the file is not badged as offline.
 *
 * This is also the seam brief §17 asks for. Encrypted local storage and a
 * biometric gate would be a third case here and a different implementation
 * behind it; no screen would change. Nothing is uploaded anywhere, ever.
 */
sealed interface DocumentAccess {

    val document: TripDocument

    /** The file is in this build and can be rendered locally. */
    data class Packaged(
        override val document: TripDocument,
        val assetPath: String,
        val mimeType: String?,
    ) : DocumentAccess

    /**
     * The package declares the document but its file is not in this build.
     *
     * A first-class state, not an error: the trip data around it — times,
     * platform, locator — is still worth showing, and often it is the part the
     * traveller actually needs at a counter.
     */
    data class NotPackaged(override val document: TripDocument) : DocumentAccess
}

/** Resolves a document to something a screen can act on, or null if unknown. */
fun documentAccess(content: TripContent, documentId: String?): DocumentAccess? {
    val document = content.trip.documents.firstOrNull { it.id == documentId } ?: return null
    val asset = content.assets.asset(document.assetId)
    val path = asset?.let { content.assets.packagedPathIfPresent(document.assetId) }
        ?: return DocumentAccess.NotPackaged(document)
    return DocumentAccess.Packaged(document, path, asset.mimeType)
}

/**
 * Whether every document the trip declares as offline actually resolves.
 *
 * Screen 13's "Tudo offline" pill is this and nothing else: a header that
 * says everything is on the phone while a document is missing is the lie D013
 * refused to tell on screen 05.
 */
fun allDocumentsResolve(content: TripContent): Boolean =
    content.trip.documents.all { documentAccess(content, it.id) is DocumentAccess.Packaged }
