package com.travelcompanion.app.feature.attraction

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.EditorialSection
import com.travelcompanion.app.data.trip.PracticalInfo
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
        // The practical price is a line, not a chip, whatever its length: the
        // rule turns on who wrote the string, not on how long it is (D154).
        assertFalse(state.chips.contains("Entrada livre"))
        assertEquals(
            listOf(PracticalLineUi("Entrada", "Entrada livre")),
            state.practicalLines,
        )
    }

    /* ------------------------------------------------ practical text (D154) */

    /**
     * The companion to `AttractionPracticalPackagedTest`, built by hand so it
     * runs on machines that do not carry the operational package.
     *
     * The string is the real one: `attr.dubrovnik.muralhas`, 141 characters,
     * whose last clause is the part a traveller needs at the gate.
     */
    @Test
    fun `a practical sentence is never truncated into a chip`() {
        val dubrovnikPass = "Dubrovnik Pass de 1 dia, €40 por pessoa, já pago — inclui as " +
            "muralhas, os museus municipais e o transporte urbano, mas não cobre o teleférico"
        val trip = content.trip
        val withLongPrice = TripContent(
            trip.copy(
                attractions = trip.attractions.map { attraction ->
                    if (attraction.id == "bascarsija") {
                        attraction.copy(
                            practical = PracticalInfo(
                                price = dubrovnikPass,
                                openingHours = "As guaritas abrem às 7h; antes disso costumam " +
                                    "estar sem atendente, com os portões abertos",
                                recommendedDurationMinutes = 60,
                            ),
                        )
                    } else {
                        attraction
                    }
                },
            ),
            AssetResolver(trip.assets, exists = { true }),
        )

        val state = buildAttractionState(withLongPrice, "bascarsija", dayDate)!!

        assertEquals(141, dubrovnikPass.length)
        assertEquals(
            "the source's own sentences, whole",
            listOf(
                PracticalLineUi("Entrada", dubrovnikPass),
                PracticalLineUi(
                    "Horários",
                    "As guaritas abrem às 7h; antes disso costumam estar sem atendente, " +
                        "com os portões abertos",
                ),
            ),
            state.practicalLines,
        )
        // The duration chip is the app's own composition and stays a chip.
        assertTrue(state.chips.contains("Visita ~60 min"))
        assertTrue(
            "a chip must never carry a sentence: ${state.chips}",
            state.chips.all { it.length <= 45 },
        )
    }

    /* --------------------------- the rest of `practical` (D176) */

    /**
     * The companion to `AttractionRequirementsPackagedTest`, built by hand so
     * it runs on machines that do not carry the operational package.
     *
     * Four of `practicalInfo`'s seven fields were parsed since the first schema
     * and read by nobody. Three of them are one sentence each and become lines
     * of the block D154 built for exactly that; `requirements` is a list and
     * gets its own block in the operational layer — never beside
     * `whatToObserve`, which is editorial, numbered in gold, and answers a
     * different question. Carrying repellent is not a curiosity about a place.
     */
    @Test
    fun `the other four practical fields all reach the state`() {
        val timedEntry = "Estar na Westermarkt às 15h20: uma vez iniciado o programa, " +
            "ninguém mais entra, e a casa não remarca nem reembolsa em nenhuma hipótese."
        val bestTime = "06:15, antes de a guarita abrir — o nascer do sol em Kotor é " +
            "por volta das 6h40"
        val accessibility = "Trilha marcada, sem guia obrigatório em tempo bom. O trecho " +
            "final não serve para quem tem vertigem severa."
        val trip = content.trip
        val withAllFour = TripContent(
            trip.copy(
                attractions = trip.attractions.map { attraction ->
                    if (attraction.id == "bascarsija") {
                        attraction.copy(
                            practical = PracticalInfo(
                                price = "Entrada livre",
                                reservationRequired = true,
                                accessibility = accessibility,
                                bestTime = bestTime,
                                requirements = listOf(timedEntry, "Sem mala nem mochila grande."),
                            ),
                        )
                    } else {
                        attraction
                    }
                },
            ),
            AssetResolver(trip.assets, exists = { true }),
        )

        val state = buildAttractionState(withAllFour, "bascarsija", dayDate)!!

        assertEquals(135, timedEntry.length)
        assertEquals(
            "the source's own sentences, whole, in the operational layer",
            listOf(timedEntry, "Sem mala nem mochila grande."),
            state.requirements,
        )
        assertEquals(
            listOf(
                PracticalLineUi("Entrada", "Entrada livre"),
                PracticalLineUi("Reserva", RESERVATION_REQUIRED),
                PracticalLineUi("Melhor hora", bestTime),
                PracticalLineUi("Acessibilidade", accessibility),
            ),
            state.practicalLines,
        )
        assertFalse(
            "a requirement is operational and never joins the gold list",
            state.whatToObserve.any { it == timedEntry },
        )
    }

    /**
     * A reservation the package says is **not** required draws nothing.
     *
     * "Não exige reserva" is a sentence nobody looks for, and an explicit
     * `false` on one attraction says no more than the field's absence on the
     * other forty-three.
     */
    @Test
    fun `a reservation declared false is not a line`() {
        val trip = content.trip
        val declaredFalse = TripContent(
            trip.copy(
                attractions = trip.attractions.map { attraction ->
                    if (attraction.id == "bascarsija") {
                        attraction.copy(practical = PracticalInfo(reservationRequired = false))
                    } else {
                        attraction
                    }
                },
            ),
            content.assets,
        )

        val state = buildAttractionState(declaredFalse, "bascarsija", dayDate)!!

        assertEquals(emptyList<PracticalLineUi>(), state.practicalLines)
    }

    /**
     * The regression guard, and the reason the other twenty-six screens are
     * safe: an attraction carrying none of the four draws exactly what it drew
     * before D176 — no block, no heading, no reserved space.
     */
    @Test
    fun `an attraction with none of the four draws what it drew before`() {
        val state = buildAttractionState(content, "latin-bridge", dayDate)!!

        assertEquals(emptyList<String>(), state.requirements)
        assertEquals(emptyList<PracticalLineUi>(), state.practicalLines)
        assertEquals(listOf("Audioguia 9 min", "Visita ~30 min"), state.chips)
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
