package com.travelcompanion.app.feature.story

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
    val stop = walkState.stops.firstOrNull { it.storyId == story.id }

    return StoryTriggerUiState(
        title = story.title,
        body = story.hook,
        fullText = story.body,
        operationalLine = listOfNotNull(
            pending.distanceMeters?.let(::distanceLabel),
            stop?.title ?: content.city(story.cityId)?.name,
            guide?.let { "áudio de ${it.durationMinutes} min" },
        ).joinToString(" · "),
        hasAudio = story.audioGuideId != null,
        footnote = FOOTNOTE,
    )
}

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
