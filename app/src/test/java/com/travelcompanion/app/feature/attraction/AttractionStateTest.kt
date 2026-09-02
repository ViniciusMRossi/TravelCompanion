package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AttractionStateTest {

    private val content = packagedContent()
    private val dayDate: LocalDate = LocalDate.parse(content.days.first().date)

    @Test
    fun theEditorialParagraphCarriesNoOperationalTiming() {
        val state = buildAttractionState(content, "latin-bridge", dayDate)!!

        assertFalse(
            "the departure time belongs to the operational strip, not the prose",
            state.summary.contains(Regex("""\d{1,2}:\d{2}""")),
        )
        assertEquals("11:00", state.departure!!.time)
    }

    @Test
    fun theWalkDepartingFromThisPlaceIsSurfaced() {
        val state = buildAttractionState(content, "latin-bridge", dayDate)!!

        val departure = state.departure!!
        assertEquals("walk.sarajevo.historical", departure.walkId)
        assertEquals(2, departure.storyCount)
        assertEquals("Baščaršija", departure.meetingPoint)
    }

    @Test
    fun audioGuideDurationComesFromContentAndOfflineStateIsHonest() {
        val state = buildAttractionState(content, "latin-bridge", dayDate)!!

        assertEquals("Ouvir audioguia · 9 min", state.audioLabel)
        assertFalse("no audio binary is packaged yet", state.audioAvailableOffline)
        assertFalse("so it must not claim to be saved", state.chips.contains("Salvo offline"))
    }

    @Test
    fun offlineChipAppearsOnlyWhenTheBinaryIsActuallyPackaged() {
        val withBinaries = packagedContent(exists = { true })

        val state = buildAttractionState(withBinaries, "latin-bridge", dayDate)!!

        assertTrue(state.audioAvailableOffline)
        assertTrue(state.chips.contains("Salvo offline"))
        assertNotNull(state.heroAssetPath)
    }

    @Test
    fun heroFallsBackToAPlaceholderCaptionWhenPhotographyIsMissing() {
        val state = buildAttractionState(content, "latin-bridge", dayDate)!!

        assertNull(state.heroAssetPath)
        assertTrue(state.heroCaption.startsWith("foto — "))
    }

    @Test
    fun anUnknownAttractionYieldsNoState() {
        assertNull(buildAttractionState(content, "does-not-exist", dayDate))
    }
}
