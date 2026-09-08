package com.travelcompanion.app.domain.editorial

import com.travelcompanion.app.data.trip.EditorialSection

/**
 * One titled stretch of narration, ready to draw.
 *
 * [paragraphs] and not one string: `editorialSection.body` is a single field in
 * the schema, and written narration carries several paragraphs inside it. The
 * split happens here rather than in the composable so that "three paragraphs
 * stay three" is a fact a unit test can hold (D133).
 *
 * It lives in `domain/` rather than in either feature because two screens draw
 * the same shape from the same `$defs/editorialSection` — screen 05 from
 * `attraction.historySections` and screen 04 from `city.historySections` — and
 * a second copy of [paragraphsOf] would drift from the first the day one of
 * the two is corrected. The precedent is `domain/operations/Contacts.kt`,
 * shared by screens 17 and 19 for the same reason.
 */
data class EditorialSectionUi(
    val title: String,
    val paragraphs: List<String>,
)

/** The package's sections, broken where their author broke them. */
fun editorialSectionsOf(sections: List<EditorialSection>): List<EditorialSectionUi> =
    sections.map { EditorialSectionUi(it.title, paragraphsOf(it.body)) }

/**
 * A section's body, broken where its author broke it.
 *
 * The schema keeps the body in one string and the written content separates
 * paragraphs with a blank line. Handing that whole string to a single `Text`
 * is three hundred words in one slab, so it is split on the blank line and
 * each piece is drawn on its own. Blank runs of any length count as one break,
 * and `\r\n` breaks exactly as `\n` does.
 */
fun paragraphsOf(body: String): List<String> =
    body.split(Regex("(\r?\n){2,}"))
        .map(String::trim)
        .filter(String::isNotEmpty)
