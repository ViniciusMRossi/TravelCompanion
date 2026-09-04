package com.travelcompanion.app.feature.planb

import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.operations.WITHHELD_NOTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/** Screen 18 from the package. */
class PlanBStateTest {

    private val content = packagedContent(exists = { false })
    private val plan = "planb.day09"

    @Test
    fun `the scenario and the reassurance come from the package`() {
        val state = buildPlanBState(content, plan, LocalTime.NOON)!!

        assertEquals("Se perderem o transporte principal do fim do dia", state.scenario)
        assertTrue(state.reassurance.isNotBlank())
        assertEquals(3, state.steps.size)
        assertEquals("Confirmar a próxima saída", state.steps.first().step.title)
    }

    /**
     * No step of this plan declares a deadline, so nothing has expired and the
     * traveller is on the first attempt whatever the clock says.
     */
    @Test
    fun `with no deadlines the first step is the current one`() {
        listOf(LocalTime.of(6, 0), LocalTime.of(23, 30)).forEach { now ->
            val steps = buildPlanBState(content, plan, now)!!.steps
            assertTrue(steps.first().isCurrent)
            assertFalse(steps[1].isCurrent)
        }
    }

    /** "Já guardado" has to mean the document really is in the package. */
    @Test
    fun `the alternatives are documents the package carries`() {
        val state = buildPlanBState(content, plan, LocalTime.NOON)!!

        assertEquals(1, state.alternatives.size)
        assertEquals("ticket.sarajevo-mostar", state.alternatives.first().documentId)
        assertEquals("Passagem Sarajevo → Mostar", state.alternatives.first().title)
    }

    /** The step that calls the hotel goes through the same gate as screen 16. */
    @Test
    fun `a phone a step declares is withheld while the content is mock`() {
        val state = buildPlanBState(content, plan, LocalTime.NOON)!!
        val phone = state.phones.getValue("tel:+387000000000")

        assertFalse(phone.dialable)
        assertNull(phone.number)
        assertEquals(WITHHELD_NOTE, phone.note)
    }

    @Test
    fun `an unknown plan is nothing to build`() {
        assertNull(buildPlanBState(content, "does.not.exist", LocalTime.NOON))
    }
}
