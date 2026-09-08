package com.travelcompanion.app.feature.emergency

import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.operations.PhoneUi
import com.travelcompanion.app.domain.operations.phone
import java.time.LocalDate

/** Screen 17 state. */
data class EmergencyUiState(
    /** Where the traveller is, in plain words and nothing else. */
    val location: String,
    val general: PhoneUi,
    /**
     * Whether the big button carries "Funciona sem crédito e sem chip local."
     *
     * The approved sheet ties that sentence to one number — *"Botão único de
     * 88dp 'Ligar 112' com a observação de que funciona sem crédito"* — and it
     * is a property of 112, not of emergency numbers in general. The real
     * package has a seventh profile whose general number is Brazil's **190**,
     * which is reached on days 1 and 20, and the note is not true of it
     * (D088).
     */
    val generalWorksWithoutCredit: Boolean,
    /**
     * What the country says about its own emergency number.
     *
     * Three of the seven packaged profiles carry one and it is operational, not
     * editorial: Bosnia's says 112 is still being rolled out and that the
     * numbers drawn beside it are what the country publishes, and Brazil's says
     * there is no single number at all. Kept out of [general]'s own
     * `PhoneUi.note`, which is already spoken for — that one stands in for a
     * number withheld as mock content, and the two would collide in exactly the
     * state where both apply (D160).
     */
    val generalNote: String?,
    val police: PhoneUi?,
    val ambulance: PhoneUi?,
    /** Insurance, the hotel where the bags are, the consulate. */
    val contacts: List<PhoneUi>,
    val showToSomeone: ShowToSomeoneUi?,
)

/** The ink card held up to a stranger, and what it says underneath. */
data class ShowToSomeoneUi(val localLanguageText: String, val translation: String)

/**
 * The one number the approved note is written about. Six of the seven packaged
 * profiles carry it; Brazil's does not.
 */
private const val EUROPEAN_EMERGENCY_NUMBER = "112"

/**
 * Builds screen 17.
 *
 * The three public services always dial: 112 and the numbers beside it are
 * facts about a country rather than about this trip, and the schema keeps them
 * in their own fields. Everything else is trip data and passes the same gate
 * as every other packaged telephone — a number that fails when pressed is
 * worse than a row that says the number is still coming (D064).
 *
 * Which country's services those are is decided by where the traveller is, not
 * by the order the profiles happen to sit in the package (D070), and *where
 * the traveller is* on a day that crosses a border is the city the day
 * declares as its base — not the first one in its list, which is the city
 * they left that morning (D090).
 */
fun buildEmergencyState(content: TripContent, date: LocalDate): EmergencyUiState? {
    val mock = content.trip.metadata.isMockContent
    // Where the traveller is, resolved exactly as screens 02 and 03 resolve
    // it: the day's declared base, and only then the first city it lists.
    // Anything else lets this screen and the timeline name different places
    // on the same day (D090).
    val city = content.dayFor(date)
        ?.let { day ->
            content.city(
                day.baseCityId
                    // fallback: a day that declares no base still has to name
                    // somewhere, and its first city is where it begins.
                    ?: day.cityIds.firstOrNull(),
            )
        }
        // fallback: a package with no days at all still has to name a country
        // for the emergency numbers below.
        ?: content.trip.cities.firstOrNull()

    // The country is the city's, and the profile is the one for that country.
    // A package that crosses a border carries a profile per country and the
    // schema requires the code on both sides; the first profile is a fallback
    // for a package that is missing one, never the normal path.
    val profile = city?.countryCode?.let(content::emergencyProfile)
        // fallback: a package that enters a country it carries no profile for
        // is incomplete; a screen with no emergency number is worse (D070).
        ?: content.trip.emergencyProfiles.firstOrNull()
        ?: return null

    // The hotel where the bags are is tonight's hotel, which the day declares
    // — the mechanism screen 03 already uses. The first accommodation in the
    // package is the right answer only on the first night (D077).
    val stay = content.dayFor(date)
        ?.accommodationIds
        ?.firstNotNullOfOrNull(content::accommodation)

    return EmergencyUiState(
        // The country name comes from the city as well, so the line cannot
        // pair one country's city with another country's name.
        location = listOfNotNull(city?.name, city?.countryName).joinToString(" · "),
        general = phone(
            label = "Ligar ${profile.generalEmergency.phone}",
            number = profile.generalEmergency.phone,
            accessibilityLabel = "Ligar para ${profile.generalEmergency.label}",
            isMockContent = mock,
            publicService = true,
        ),
        generalWorksWithoutCredit = profile.generalEmergency.phone == EUROPEAN_EMERGENCY_NUMBER,
        // Only the general number's note is drawn. `note` does not mean the
        // same thing on every contact: on the insurer and the consulate of the
        // sample package it is an authoring instruction — "Substituir pelo
        // contato real" — which is why those rows have never shown one, and
        // why this is wired one field at a time rather than for `note` at
        // large (D160).
        generalNote = profile.generalEmergency.note?.takeIf { it.isNotBlank() },
        police = profile.police?.let {
            phone(
                label = it.label,
                number = it.phone,
                accessibilityLabel = "Ligar para ${it.label}",
                isMockContent = mock,
                publicService = true,
            )
        },
        ambulance = profile.ambulance?.let {
            phone(
                label = it.label,
                number = it.phone,
                accessibilityLabel = "Ligar para ${it.label}",
                isMockContent = mock,
                publicService = true,
            )
        },
        contacts = listOfNotNull(
            // The approved row names the policy number beside the insurer.
            // `emergencyContact` has no field for one — its `note` is an
            // authoring instruction ("Substituir pelo contato real") and not
            // something to show a traveller — so the row carries the label
            // alone rather than inventing a number or a field.
            profile.insurance?.let {
                phone(
                    label = it.label,
                    number = it.phone,
                    accessibilityLabel = "Ligar para ${it.label}",
                    isMockContent = mock,
                )
            },
            // Everything on this screen is either a number to call or a
            // sentence to show a stranger. A stay the package carries no
            // number for is neither, so it does not take a row — the same
            // `contactPhone?.let` screen 16 already uses (D109).
            stay?.contactPhone?.let { number ->
                phone(
                    label = stay.name,
                    number = number,
                    detail = "Onde estão as malas",
                    accessibilityLabel = "Ligar para a hospedagem, ${stay.name}",
                    isMockContent = mock,
                )
            },
            profile.consular?.let {
                phone(
                    label = it.label,
                    number = it.phone,
                    accessibilityLabel = "Ligar para ${it.label}",
                    isMockContent = mock,
                )
            },
        ),
        showToSomeone = profile.showToSomeone?.let {
            ShowToSomeoneUi(it.localLanguageText, it.translation)
        },
    )
}
