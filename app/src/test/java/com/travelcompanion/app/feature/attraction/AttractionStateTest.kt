package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.EditorialSection
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
    fun audioGuideDurationComesFromContentNotFromAConstant() {
        val state = bascarsija()

        // The approved prototype states 12 min, and the packaged placeholder
        // runs the full 720s, so the label and the file agree.
        assertEquals("Ouvir audioguia · 12 min", state.audioLabel)
    }

    @Test
    fun offlineStateIsHonestWhenTheBinaryIsAbsent() {
        val withoutBinaries = packagedContent(exists = { false })

        val state = buildAttractionState(withoutBinaries, "bascarsija", dayDate)!!

        assertFalse(state.audioAvailableOffline)
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

    /* ------------------------------------------- editorial sections (D133) */

    /** The packaged content with `bascarsija` carrying the given sections. */
    private fun withSections(vararg sections: EditorialSection): TripContent {
        val trip = content.trip
        return TripContent(
            trip.copy(
                attractions = trip.attractions.map { attraction ->
                    if (attraction.id == "bascarsija") {
                        attraction.copy(historySections = sections.toList())
                    } else {
                        attraction
                    }
                },
            ),
            content.assets,
        )
    }

    /**
     * `historySections` was parsed since the first schema and read by nobody:
     * 127 written sections would have entered the package and reached no
     * screen at all. Screen 05 now carries them (D133).
     */
    @Test
    fun theEditorialSectionsReachTheState() {
        val withTwo = withSections(
            EditorialSection("O bazar otomano", "Fundado em 1462 pelo governador."),
            EditorialSection("A fonte no meio", "O Sebilj que está ali hoje é de 1891."),
        )

        val state = buildAttractionState(withTwo, "bascarsija", dayDate)!!

        assertEquals(2, state.historySections.size)
        assertEquals(
            listOf("O bazar otomano", "A fonte no meio"),
            state.historySections.map { it.title },
        )
        assertEquals(
            listOf("Fundado em 1462 pelo governador."),
            state.historySections.first().paragraphs,
        )
        assertEquals(
            listOf("O Sebilj que está ali hoje é de 1891."),
            state.historySections.last().paragraphs,
        )
    }

    /**
     * The schema keeps a section's body in one string, and the written content
     * carries several paragraphs inside it, separated by a blank line. Handing
     * that string to one `Text` is three hundred words in a single slab, so the
     * body is split where the author broke it (D133).
     */
    @Test
    fun aBodyOfThreeParagraphsDoesNotCollapseIntoOne() {
        val threeParagraphs = withSections(
            EditorialSection(
                "A cidade e o cerco",
                """
                    Primeiro parágrafo, sobre o começo.

                    Segundo parágrafo, sobre o meio.

                    Terceiro parágrafo, sobre o fim.
                """.trimIndent(),
            ),
        )

        val paragraphs = buildAttractionState(threeParagraphs, "bascarsija", dayDate)!!
            .historySections
            .single()
            .paragraphs

        assertEquals(3, paragraphs.size)
        assertEquals("Primeiro parágrafo, sobre o começo.", paragraphs[0])
        assertEquals("Segundo parágrafo, sobre o meio.", paragraphs[1])
        assertEquals("Terceiro parágrafo, sobre o fim.", paragraphs[2])
    }

    /** Windows line endings break a paragraph exactly as Unix ones do. */
    @Test
    fun aBodySplitWithCarriageReturnsSplitsTheSameWay() {
        val crlf = withSections(
            EditorialSection("t", "Um." + "\r\n" + "\r\n" + "Dois."),
        )

        assertEquals(
            listOf("Um.", "Dois."),
            buildAttractionState(crlf, "bascarsija", dayDate)!!
                .historySections.single().paragraphs,
        )
    }

    /** A single-paragraph body is one paragraph, not one-and-an-empty. */
    @Test
    fun aBodyOfOneParagraphStaysOne() {
        val one = withSections(EditorialSection("t", "Um parágrafo só, sem quebra."))

        assertEquals(
            listOf("Um parágrafo só, sem quebra."),
            buildAttractionState(one, "bascarsija", dayDate)!!
                .historySections.single().paragraphs,
        )
    }

    /**
     * The regression guard. Every attraction in the packaged trip has no
     * sections, and so do all 27 in the real package: adding this block must
     * leave those screens byte-for-byte as they were.
     */
    @Test
    fun anAttractionWithNoSectionsDrawsNothingAndChangesNothingElse() {
        val state = bascarsija()

        assertTrue(state.historySections.isEmpty())
        // The rest of the screen is untouched: same summary, same strip, same
        // observations, same Plan B.
        assertEquals(content.attraction("bascarsija")!!.summary, state.summary)
        assertEquals("11:00", state.departure!!.time)
        assertTrue(state.whatToObserve.isNotEmpty())
        assertNotNull(state.planBTitle)
        // And the state with an empty section list is the state as it was.
        assertEquals(state, buildAttractionState(withSections(), "bascarsija", dayDate))
    }

    @Test
    fun anUnknownAttractionYieldsNoState() {
        assertNull(buildAttractionState(content, "does-not-exist", dayDate))
    }
}
