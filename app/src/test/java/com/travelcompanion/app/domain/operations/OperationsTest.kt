package com.travelcompanion.app.domain.operations

import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.data.trip.PlanB
import com.travelcompanion.app.data.trip.PlanBStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * The two decisions the operational screens make, against a fixed clock.
 *
 * The act-by time and the nominal time are different facts — "esteja na
 * estação até 19:00" is not "o ônibus sai às 19:30" — and the approved screens
 * never merge them, so neither does this.
 */
class OperationsTest {

    private val bus = CriticalItem(
        id = "critical.bus",
        title = "Ônibus para Mostar",
        nominalTime = "19:30",
        actionByTime = "19:00",
        instruction = "Esteja na estação até 19:00.",
        reason = "Evitar risco de perder o embarque.",
    )

    @Test
    fun `the window opens at the act-by time and closes at the nominal time`() {
        assertEquals(ActionWindow.Ahead, actionWindow(bus, LocalTime.of(18, 59)))
        assertEquals(ActionWindow.Now, actionWindow(bus, LocalTime.of(19, 0)))
        assertEquals(ActionWindow.Now, actionWindow(bus, LocalTime.of(19, 29)))
        assertEquals(ActionWindow.Passed, actionWindow(bus, LocalTime.of(19, 30)))
        assertEquals(ActionWindow.Passed, actionWindow(bus, LocalTime.of(23, 0)))
    }

    /** The stay's own critical item has an act-by time and no nominal one. */
    @Test
    fun `an item with only an act-by time never reads as passed`() {
        val reception = CriticalItem(
            id = "critical.reception",
            title = "Recepção fecha às 23:00",
            actionByTime = "22:45",
            instruction = "Ligue para o anfitrião antes de 22:45.",
        )

        assertEquals(ActionWindow.Ahead, actionWindow(reception, LocalTime.of(22, 44)))
        assertEquals(ActionWindow.Now, actionWindow(reception, LocalTime.of(22, 45)))
        assertEquals(ActionWindow.Now, actionWindow(reception, LocalTime.of(23, 59)))
    }

    @Test
    fun `an item with no times at all is never urgent`() {
        val vague = CriticalItem(id = "c", title = "Algo", instruction = "…")
        assertEquals(ActionWindow.Ahead, actionWindow(vague, LocalTime.of(12, 0)))
    }

    // -- which Plan B step is still worth trying --------------------------

    private fun plan(vararg deadlines: String?) = PlanB(
        id = "planb",
        scenario = "Se perderem o ônibus",
        reassurance = "Ninguém fica na rua.",
        steps = deadlines.mapIndexed { index, deadline ->
            PlanBStep(title = "Passo $index", instruction = "…", deadline = deadline)
        },
    )

    @Test
    fun `the current step is the first whose deadline has not passed`() {
        val p = plan("19:00", "21:00", "23:00")
        assertEquals("Passo 0", currentStep(p, LocalTime.of(18, 0))?.title)
        assertEquals("Passo 1", currentStep(p, LocalTime.of(19, 30))?.title)
        assertEquals("Passo 2", currentStep(p, LocalTime.of(22, 0))?.title)
        assertEquals(2, currentStepIndex(p, LocalTime.of(22, 0)))
    }

    /** Nothing has expired when nothing declares a deadline. */
    @Test
    fun `steps without deadlines start at the first`() {
        val p = plan(null, null, null)
        assertEquals("Passo 0", currentStep(p, LocalTime.of(23, 59))?.title)
    }

    @Test
    fun `every deadline passed leaves no step current`() {
        val p = plan("06:00", "07:00")
        assertNull(currentStep(p, LocalTime.of(12, 0)))
        assertNull(currentStepIndex(p, LocalTime.of(12, 0)))
    }

    // -- telephones -------------------------------------------------------

    /**
     * D061's argument with a higher price: a button that dials `+000000000`
     * fails at the moment it is pressed, which is the moment it was kept for.
     */
    @Test
    fun `a trip phone is withheld while the package is mock`() {
        val insurer = phone(
            label = "Seguro viagem",
            number = "+000000000",
            isMockContent = true,
        )

        assertFalse(insurer.dialable)
        assertNull(insurer.number)
        assertEquals(WITHHELD_NOTE, insurer.note)
        assertEquals("the contact still has to be visible", "Seguro viagem", insurer.label)
    }

    @Test
    fun `a real package dials the same number`() {
        val insurer = phone(label = "Seguro viagem", number = "+5511999998888", isMockContent = false)
        assertTrue(insurer.dialable)
        assertEquals("+5511999998888", insurer.number)
        assertNull(insurer.note)
    }

    /**
     * 112 is a fact about a country, not about this trip, so it dials whatever
     * state the package is in.
     */
    @Test
    fun `public emergency numbers are never withheld`() {
        val emergency = phone(
            label = "Ligar 112",
            number = "112",
            isMockContent = true,
            publicService = true,
        )
        assertTrue(emergency.dialable)
        assertEquals("112", emergency.number)
    }

    @Test
    fun `a missing number is withheld even in a real package`() {
        assertFalse(phone(label = "Consulado", number = null, isMockContent = false).dialable)
        assertFalse(phone(label = "Consulado", number = "  ", isMockContent = false).dialable)
    }
}
