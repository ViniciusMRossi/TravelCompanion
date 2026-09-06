package com.travelcompanion.app.feature.story

import com.travelcompanion.app.data.trip.Story
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.walk.WalkModeState
import java.util.Locale

/** Screen 10 state. */
data class StoryTriggerUiState(
    val title: String,
    /** The paragraph the sheet opens with. */
    val body: String,
    /** The whole story, for *Ler* — screen 10 reads it where it stands. */
    val fullText: String,
    /** Distance, exact place and the audio's length, kept off the editorial. */
    val operationalLine: String,
    val hasAudio: Boolean,
    val footnote: String,
)

/**
 * Builds screen 10 — a story offered by arriving somewhere.
 *
 * It is only ever built for a story the decision *offered*: one that played by
 * itself is already in the traveller's ears, and raising a page for it would
 * light a screen that Walk Mode assumes is off (D078).
 *
 * The composition keeps the walk above and the story below, and the walk above
 * is screen 07 itself rather than a copy of it. What this keeps is the other
 * half of the same separation: the operational facts — how far, where exactly,
 * how long the audio runs — are one line of their own, never folded into the
 * paragraph.
 */
fun buildStoryTriggerState(
    content: TripContent,
    walkState: WalkModeState,
): StoryTriggerUiState? {
    val pending = walkState.pending ?: return null
    val story = content.story(pending.storyId) ?: return null
    val guide = content.audioGuide(story.audioGuideId)

    return StoryTriggerUiState(
        title = story.title,
        body = story.hook,
        fullText = story.body,
        operationalLine = listOfNotNull(
            pending.distanceMeters?.let(::distanceLabel),
            placeOf(content, story),
            guide?.let { "áudio de ${it.durationMinutes} min" },
        ).joinToString(" · "),
        hasAudio = story.audioGuideId != null,
        footnote = FOOTNOTE,
    )
}

/**
 * The *place*, which is the middle field the approved sheet draws there: "A
 * 20 m de você · **Ponte Latina, margem norte** · 6 min de áudio".
 *
 * It used to read `stop.title ?: city.name`, and the walk stop's title is
 * `story.title` by construction (`prepareWalk`) — so the field could only ever
 * repeat the headline two lines above it. With the real package that printed
 * "a 0 m · O Sebilj tem 1891; a praça tem 1462 · áudio de 3 min" directly
 * under an h1 saying the same thing: **the same line twice on one sheet**,
 * which is D102's defect one screen over, and the second time this repository
 * has shipped it.
 *
 * So the stop is not consulted at all, and the city is the place. It is less
 * specific than the sheet draws, and that is a content limit rather than a
 * layout one: no field in the schema carries "margem norte", and inventing one
 * is a content decision this did not have to make.
 */
private fun placeOf(content: TripContent, story: Story): String? =
    content.city(story.cityId)?.name

/** "a 40 m" / "a 1,2 km". */
private fun distanceLabel(meters: Int): String =
    if (meters >= 1_000) {
        String.format(Locale.forLanguageTag("pt-BR"), "a %.1f km", meters / 1_000.0)
    } else {
        "a $meters m"
    }

/**
 * What the traveller is told about how this reached them.
 *
 * Walk Mode assumes headphones and a dark screen, so the screen says the chime
 * was the whole of it and the phone can stay where it is.
 */
private const val FOOTNOTE =
    "O aviso chegou como um toque suave nos fones. O celular pode continuar no bolso."
