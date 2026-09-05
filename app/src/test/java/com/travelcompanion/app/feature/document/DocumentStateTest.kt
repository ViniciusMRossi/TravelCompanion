package com.travelcompanion.app.feature.document

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screen 14 built from the package, including the two states the package
 * actually produces today: a file that is not in the build, and ticket data
 * that is mock.
 */
class DocumentStateTest {

    private val whole = packagedContent(exists = { true })
    private val asShipped = packagedContent(exists = { false })

    @Test
    fun `the ticket is drawn from trip data, not from the file`() {
        val state = buildDocumentState(asShipped, "ticket.sarajevo-mostar")!!

        assertEquals("Passagem Sarajevo → Mostar", state.title)
        assertEquals("19:30", state.journey?.originTime)
        assertEquals("22:00", state.journey?.destinationTime)
        assertEquals("Vinícius · Érika", state.passengers)
        assertEquals("MOCK-ABC123", state.locator)
        assertNotNull("the redundancy line is the point of the ficha", state.locatorNote)
    }

    /**
     * D013's rule, on screen 14: the promise of offline without the file is
     * not a failure to open, it is a state that says so and keeps showing the
     * data that is on the phone.
     */
    @Test
    fun `a document whose file is not packaged still shows its data`() {
        val state = buildDocumentState(asShipped, "ticket.sarajevo-mostar")!!

        assertFalse(state.isPackaged)
        assertNotNull(state.notPackagedNote)
        assertEquals("19:30", state.journey?.originTime)
    }

    @Test
    fun `a packaged document says nothing about being missing`() {
        val state = buildDocumentState(whole, "ticket.sarajevo-mostar")!!
        assertTrue(state.isPackaged)
        assertNull(state.notPackagedNote)
    }

    /**
     * The file the "Abrir arquivo" control hands to a viewer (D091).
     *
     * The control exists only when the file does, which is the same rule the
     * QR control follows: one that opens nothing is worse than none. The name
     * is the document's own, because that is what the viewer's title bar reads
     * — never the asset's build path.
     */
    @Test
    fun `a packaged document offers its file, named as the document is`() {
        val state = buildDocumentState(whole, "ticket.sarajevo-mostar")!!

        val file = state.file
        assertNotNull(file)
        assertEquals("trip/documents/tickets/sarajevo-mostar.pdf", file!!.assetPath)
        assertTrue(
            "the viewer must not be handed a build path as a title",
            file.displayName.endsWith(".pdf") && !file.displayName.contains('/'),
        )
    }

    @Test
    fun `a document with no file in this build offers none`() {
        assertNull(buildDocumentState(asShipped, "ticket.sarajevo-mostar")!!.file)
    }

    // -- the QR gate ------------------------------------------------------

    /**
     * §18: a QR must come from the actual document or actual ticket data. The
     * package is `isMockContent`, so `MOCK-ABC123` is not encoded — a
     * scannable square holding a fake locator is worse at a counter than no
     * square at all (D061).
     */
    @Test
    fun `mock ticket data does not become a scannable code`() {
        val state = buildDocumentState(whole, "ticket.sarajevo-mostar")!!
        assertEquals(QrState.MockContent, state.qr)
    }

    /**
     * The other half of the gate. D061 guarded only the generated case, and
     * §18's words are "never use a *visual* placeholder QR" — a packaged
     * image is that case, and is if anything more convincing (D063).
     */
    @Test
    fun `a packaged code is refused too while the content is mock`() {
        val packaged = packagedContent(exists = { true })
        val withEmbedded = packaged.trip.copy(
            documents = packaged.trip.documents.map { document ->
                if (document.id == "ticket.sarajevo-mostar") {
                    document.copy(
                        qr = com.travelcompanion.app.data.trip.DocumentQr(
                            mode = "embedded",
                            embeddedAssetId = "img.latin-bridge.hero",
                        ),
                    )
                } else {
                    document
                }
            },
        )
        val content = TripContent(withEmbedded, packaged.assets)

        val state = buildDocumentState(content, "ticket.sarajevo-mostar")!!
        assertEquals(QrState.MockContent, state.qr)
    }

    @Test
    fun `a document that declares no code says so plainly`() {
        val state = buildDocumentState(whole, "voucher.sarajevo")!!
        assertEquals(QrState.None, state.qr)
    }

    /**
     * `embedded` and `generated-from-text` are both "there is a code" and are
     * deliberately different values: collapsing them into one string would
     * have encoded the asset's *path* into a QR.
     */
    @Test
    fun `an embedded code is a packaged image, not something to encode`() {
        val qr = QrState.Embedded("trip/documents/qr.png")
        assertTrue(qr is QrState.Ready)
        assertFalse(qr is QrState.Generated)
    }

    @Test
    fun `an unknown document is nothing to build`() {
        assertNull(buildDocumentState(whole, "does.not.exist"))
        assertNull(buildDocumentState(whole, null))
    }
}
