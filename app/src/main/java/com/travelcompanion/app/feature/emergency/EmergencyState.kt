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
    val police: PhoneUi?,
    val ambulance: PhoneUi?,
    /** Insurance, the hotel where the bags are, the consulate. */
    val contacts: List<PhoneUi>,
    val showToSomeone: ShowToSomeoneUi?,
)

/** The ink card held up to a stranger, and what it says underneath. */
data class ShowToSomeoneUi(val localLanguageText: String, val translation: String)

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
 * by the order the profiles happen to sit in the package (D070).
 */
fun buildEmergencyState(content: TripContent, date: LocalDate): EmergencyUiState? {
    val mock = content.trip.metadata.isMockContent
    // Where the traveller is, from the day the current-day logic resolves —
    // the same one screens 02 and 13 use, not a second rule.
    val city = content.dayFor(date)
        ?.let { day -> content.city(day.cityIds.firstOrNull()) }
        ?: content.trip.cities.firstOrNull()

    // The country is the city's, and the profile is the one for that country.
    // A package that crosses a border carries a profile per country and the
    // schema requires the code on both sides; the first profile is a fallback
    // for a package that is missing one, never the normal path.
    val profile = city?.countryCode?.let(content::emergencyProfile)
        ?: content.trip.emergencyProfiles.firstOrNull()
        ?: return null

    val stay = content.trip.accommodations.firstOrNull()

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
            stay?.let {
                phone(
                    label = it.name,
                    number = it.contactPhone,
                    detail = "Onde estão as malas",
                    accessibilityLabel = "Ligar para a hospedagem, ${it.name}",
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
