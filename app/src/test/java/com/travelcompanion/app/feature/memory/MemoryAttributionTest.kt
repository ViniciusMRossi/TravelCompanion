package com.travelcompanion.app.feature.memory

import com.travelcompanion.app.data.trip.TwoCityTrip
import com.travelcompanion.app.domain.walk.WalkModeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Where a voice memory says it happened.
 *
 * This is the one thing in the app that cannot be redone — it is written into
 * the memory and kept — so the city on it has to be the city the traveller is
 * in, not the first one in the package (D077).
 */
class MemoryAttributionTest {

    private val content = TwoCityTrip.content()

    @Test
    fun `a memory is attributed to the city of the day it is recorded`() {
        val second = buildAttribution(
            content = content,
            walkState = WalkModeState(),
            localParticipantId = "vinicius",
            date = TwoCityTrip.DAY_TWO,
        )!!

        assertEquals("Mostar", second.cityName)
    }

    @Test
    fun `the first day is still the first city`() {
        val first = buildAttribution(
            content = content,
            walkState = WalkModeState(),
            localParticipantId = "vinicius",
            date = TwoCityTrip.DAY_ONE,
        )!!

        assertEquals("Sarajevo", first.cityName)
        assertEquals("Vinícius", first.participantName)
    }

    /** Without a participant there is nobody to attribute a memory to. */
    @Test
    fun `no participant is nothing to attribute`() {
        assertNull(
            buildAttribution(content, WalkModeState(), null, TwoCityTrip.DAY_ONE),
        )
    }
}
