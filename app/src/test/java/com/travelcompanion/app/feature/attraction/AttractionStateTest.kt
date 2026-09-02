package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
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

    /** The attraction the approved prototype opens from Today. */
    private fun bascarsija() = buildAttractionState(content, "bascarsija", dayDate)!!

    @Test
    fun theEditorialParagraphCarriesNoOperationalTiming() {
        val state = bascarsija()

        assertFalse(
            "the departure time belongs to the operational strip, not the prose",
            state.summary.contains(Regex("""\d{1,2}:\d{2}""")),
        )
        assertEquals("11:00", state.departure!!.time)
    }

    @Test
    fun theWalkIsOfferedAtTheAttractionItActuallyDepartsFrom() {
        val state = bascarsija()

        val departure = state.departure!!
        assertEquals("walk.sarajevo.historical", departure.walkId)
        assertEquals("Fonte Sebilj", departure.meetingPoint)
        assertEquals(2, departure.storyCount)
    }

    @Test
    fun anAttractionThatMerelySharesTheCityDoesNotStartTheWalk() {
        // Latin Bridge is in Sarajevo and appears on the same walk as a story,
        // but the walk does not depart from it. Screen 05 must not claim it does.
        val latinBridge = buildAttractionState(content, "latin-bridge", dayDate)!!

        assertNull(
            "sharing a city is not an association",
            latinBridge.departure,
        )
    }

    @Test
    fun theWalkIsNotOfferedOnADayThatDoesNotScheduleIt() {
        // Same attraction, same explicit link — but the day no longer runs the
        // walk, so there is nothing for the fixed bar to start.
        val withoutTheWalk = TripContent(
            trip = content.trip.copy(
                days = content.trip.days.map { day ->
                    day.copy(timeline = day.timeline.filterNot { it.kind == "walk" })
                },
            ),
            assets = content.assets,
        )

        val state = buildAttractionState(withoutTheWalk, "bascarsija", dayDate)!!

        assertNull(state.departure)
    }

    @Test
    fun audioGuideDurationComesFromContentAndOfflineStateIsHonest() {
        val state = bascarsija()

        assertEquals("Ouvir audioguia · 12 min", state.audioLabel)
        assertFalse("no audio binary is packaged yet", state.audioAvailableOffline)
        assertFalse("so it must not claim to be saved", state.chips.contains("Salvo offline"))
    }

    @Test
    fun offlineChipAppearsOnlyWhenTheBinaryIsActuallyPackaged() {
        val withBinaries = packagedContent(exists = { true })

        val state = buildAttractionState(withBinaries, "bascarsija", dayDate)!!

        assertTrue(state.audioAvailableOffline)
        assertTrue(state.chips.contains("Salvo offline"))
        assertNotNull(state.heroAssetPath)
    }

    @Test
    fun heroFallsBackToAPlaceholderCaptionWhenPhotographyIsMissing() {
        val state = bascarsija()

        assertNull(state.heroAssetPath)
        assertTrue(state.heroCaption.startsWith("foto — "))
    }

    @Test
    fun theApprovedAttractionCarriesItsOwnPlanBAndObservations() {
        val state = bascarsija()

        assertEquals("Se chover forte", state.planBTitle)
        assertEquals(3, state.whatToObserve.size)
        assertTrue(state.chips.contains("Entrada livre"))
    }

    @Test
    fun anUnknownAttractionYieldsNoState() {
        assertNull(buildAttractionState(content, "does-not-exist", dayDate))
    }
}
