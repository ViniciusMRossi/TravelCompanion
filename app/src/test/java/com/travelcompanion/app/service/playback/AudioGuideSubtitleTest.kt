package com.travelcompanion.app.service.playback

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The floor under every caller: a subtitle equal to the title is not a
 * subtitle, it is the same line printed twice.
 *
 * Six call sites hand a subtitle in — screens 05 and 04, the story on 04,
 * Together and Walk Mode — and city guides are still to come. Discarding the
 * repetition belongs here, once, rather than in each of them (D151).
 *
 * Discarding is only the floor. What the attraction screen should say instead
 * is [com.travelcompanion.app.feature.attraction.AttractionLockScreenTest]'s
 * subject: these two fixes are separate and both are needed.
 */
class AudioGuideSubtitleTest {

    private val content = packagedContent(exists = { true })
    private val guideId = "ag.bascarsija"
    private val guideTitle = "Audioguia de Baščaršija"

    private fun subtitleFor(subtitle: String?): String? =
        (audioGuideRequest(content, guideId, subtitle = subtitle) as AudioGuideRequest.Playable)
            .subtitle

    @Test
    fun aSubtitleEqualToTheTitleIsDiscarded() {
        assertNull(subtitleFor(guideTitle))
    }

    @Test
    fun aSubtitleThatDiffersOnlyBySpacingOrCaseIsAlsoDiscarded() {
        // The package carries story titles that end in a space, and "equal
        // but for a space" is still the same line printed twice.
        assertNull(subtitleFor("$guideTitle "))
        assertNull(subtitleFor(" $guideTitle"))
        assertNull(subtitleFor(guideTitle.uppercase()))
    }

    @Test
    fun aSubtitleThatSaysSomethingElseIsKept() {
        assertEquals("Sarajevo · Bósnia e Herzegovina", subtitleFor("Sarajevo · Bósnia e Herzegovina"))
    }

    @Test
    fun noSubtitleStaysNoSubtitle() {
        assertNull(subtitleFor(null))
    }
}
