package com.travelcompanion.app.domain.wallet

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.today.BookingStatusUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Screen 13's grouping, decided against a date and nothing else.
 *
 * The package's own two documents are the fixture on purpose: they are the
 * pair that has been failing `validate_trip` since Phase 1 — both promising
 * offline, neither with its file in the build — and that is the case these
 * screens exist to handle rather than to hide (D013, D060).
 */
class WalletTest {

    private val tripDay = LocalDate.parse("2026-09-21")

    /** Everything resolves: what the wallet looks like when the build is whole. */
    private val whole = packagedContent(exists = { true })

    /** The package as it actually ships: both documents declared, neither present. */
    private val asShipped = packagedContent(exists = { false })

    @Test
    fun `documents valid on the day are today's`() {
        val state = buildWalletState(whole, tripDay)

        val today = state.sections.single { it.group == WalletGroup.Today }
        assertEquals(
            listOf("ticket.sarajevo-mostar", "voucher.sarajevo"),
            today.rows.map { it.documentId },
        )
        assertEquals(2, state.totalCount)
    }

    /**
     * Before the trip starts, `dayFor` clamps to the first day — the current-day
     * logic Phase 1 already had, which this reuses rather than repeating.
     */
    @Test
    fun `a date before the trip still resolves to a trip day`() {
        val state = buildWalletState(whole, LocalDate.parse("2026-01-01"))
        assertTrue(state.sections.any { it.group == WalletGroup.Today })
    }

    /**
     * A document the day does not cover falls out of "Hoje".
     *
     * `dayFor` clamps to a real trip day whatever date it is given, so the
     * only way to reach this is a document whose validity sits away from the
     * trip — which is what the fixture below builds. The first version of this
     * test asserted against a day the documents *do* cover and its comment
     * said otherwise, which made it read as coverage it never had.
     */
    @Test
    fun `documents that do not cover the day are not today's`() {
        val moved = whole.trip.copy(
            documents = whole.trip.documents.map {
                it.copy(validFrom = "2030-01-01", validUntil = "2030-01-02")
            },
        )
        val state = buildWalletState(TripContent(moved, whole.assets), tripDay)

        assertTrue(
            "nothing is today's when nothing covers today",
            state.sections.none { it.group == WalletGroup.Today },
        )
        // The ticket is still a transport document; the voucher is neither.
        assertEquals(
            listOf(WalletGroup.Transport, WalletGroup.Papers),
            state.sections.map { it.group },
        )
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `booking status comes from whatever owns the document`() {
        val rows = buildWalletState(whole, tripDay).sections.flatMap { it.rows }

        // The transport is `reserved`, the stay is `paid`.
        assertEquals(
            BookingStatusUi.Reserved,
            rows.single { it.documentId == "ticket.sarajevo-mostar" }.status,
        )
        assertEquals(
            BookingStatusUi.Paid,
            rows.single { it.documentId == "voucher.sarajevo" }.status,
        )
    }

    // -- the loop D013 opened ---------------------------------------------

    @Test
    fun `the header cannot say everything is offline when a file is missing`() {
        assertTrue("a whole build may say it", buildWalletState(whole, tripDay).allOffline)
        assertFalse(
            "the package as shipped may not",
            buildWalletState(asShipped, tripDay).allOffline,
        )
    }

    @Test
    fun `a document promising offline without its file says so on its own row`() {
        val rows = buildWalletState(asShipped, tripDay).sections.flatMap { it.rows }
        assertTrue("every row is unresolved in this build", rows.none { it.resolvesOffline })

        val whole = buildWalletState(whole, tripDay).sections.flatMap { it.rows }
        assertTrue(whole.all { it.resolvesOffline })
    }
}
