package com.travelcompanion.app.domain.operations

import com.travelcompanion.app.data.trip.TripContent

/** One telephone the app offers, and whether it may actually be dialled. */
data class PhoneUi(
    val label: String,
    /** Policy number, "onde estão as malas", whatever names it further. */
    val detail: String?,
    val number: String?,
    /** What to read to a screen reader; the approved screen 17 asks for its own. */
    val accessibilityLabel: String,
    val dialable: Boolean,
    /** Said in place of the number when it is withheld, and null otherwise. */
    val note: String?,
    /**
     * What the package says about this contact, when it says anything.
     *
     * Distinct from [note] on purpose: that one stands in for a number
     * withheld as mock content and is written by the app, while this one is
     * packaged text about the contact itself — why the consular telephone for
     * Montenegro answers in Belgrade, say. A row can carry one, the other, or
     * both, which is exactly why they are not the same field (D160, D175).
     */
    val contentNote: String? = null,
)

/**
 * What a packaged telephone number is allowed to become.
 *
 * D061 refused to encode mock ticket data into a scannable code, on the
 * grounds that a placeholder which looks real is worse at a counter than
 * nothing at all. A telephone is the same argument with a higher price: the
 * package ships `+000000000` for the insurer and the consulate, and
 * `+387000000000` for the host, each with a note saying to replace it. A
 * button that dials one of those fails at the moment it is pressed, which is
 * the moment it was kept for (D064).
 *
 * Public emergency services are not this kind of data. 112, and the police and
 * ambulance numbers beside it, are facts about a country rather than about
 * this trip, and they are dialled whatever state the package is in — which is
 * why they come through [publicService].
 *
 * A number simply absent from a shipped package is a third state. Five of the
 * ten stays the real trip carries have no `contactPhone` at all, and there
 * nothing is arriving later: the row cannot be dialled and has nothing to
 * promise, so it gets no note (D109).
 */
fun phone(
    label: String,
    number: String?,
    detail: String? = null,
    accessibilityLabel: String = label,
    isMockContent: Boolean,
    publicService: Boolean = false,
    contentNote: String? = null,
): PhoneUi {
    val withheldAsMock = isMockContent && !publicService
    val dialable = !withheldAsMock && !number.isNullOrBlank()
    return PhoneUi(
        label = label,
        detail = detail,
        number = number.takeIf { dialable },
        accessibilityLabel = accessibilityLabel,
        dialable = dialable,
        note = if (withheldAsMock) WITHHELD_NOTE else null,
        // Blank is the same as absent for packaged prose; the emptiness check
        // lives here so no caller has to remember it.
        contentNote = contentNote?.takeIf { it.isNotBlank() },
    )
}

/**
 * Written for a state the approved sheets do not draw, in the register of the
 * one D061 already established on screen 14: what is missing, and when it
 * arrives. The contact is still listed, because knowing an insurance line
 * exists matters even when this build cannot dial it.
 *
 * It is a sentence about mock content and only about that. It never stands in
 * for a number a real package does not have (D109).
 */
const val WITHHELD_NOTE: String = "O número chega com os dados reais da viagem."

/** Convenience: the package's own mock flag, so screens do not reach for it. */
val TripContent.isMockContent: Boolean get() = trip.metadata.isMockContent
