package com.travelcompanion.app.feature.memory

import com.travelcompanion.app.data.trip.TwoCityTrip
import com.travelcompanion.app.domain.memory.MemoryRecordingState
import com.travelcompanion.app.domain.walk.WalkModeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The line at the top of screen 12, which promises that the place arrives by
 * itself.
 *
 * It used to name the first city of the trip while the attribution beneath it
 * named the city of the day — so the same screen said two different places
 * seconds apart, which is D040's class (D080).
 */
class MemoryContextTest {

    private val content = TwoCityTrip.content()
    private val zone: ZoneId = ZoneId.systemDefault()

    private fun epochOn(day: java.time.LocalDate): Long =
        ZonedDateTime.of(day, LocalTime.of(10, 30), zone).toInstant().toEpochMilli()

    private fun state(day: java.time.LocalDate) = buildMemoryState(
        content = content,
        walkState = WalkModeState(),
        recording = MemoryRecordingState(),
        saved = emptyList(),
        localParticipantId = "vinicius",
        elapsedMs = 0L,
        nowEpochMs = epochOn(day),
        zone = zone,
    )

    @Test
    fun `the context line names the city of the day being lived`() {
        assertTrue(state(TwoCityTrip.DAY_ONE).contextLine.startsWith("Sarajevo · "))
        assertTrue(state(TwoCityTrip.DAY_TWO).contextLine.startsWith("Mostar · "))
    }

    /**
     * The two lines are built from the same answer, so the screen cannot say
     * one city before recording starts and another one after.
     */
    @Test
    fun `the context line and the attribution agree on the city`() {
        val attribution = buildAttribution(
            content = content,
            walkState = WalkModeState(),
            localParticipantId = "vinicius",
            date = TwoCityTrip.DAY_TWO,
        )!!

        assertEquals("Mostar", attribution.cityName)
        assertTrue(state(TwoCityTrip.DAY_TWO).contextLine.startsWith("${attribution.cityName} · "))
    }

    @Test
    fun `the hour is always there, whatever the package knows about place`() {
        assertTrue(state(TwoCityTrip.DAY_ONE).contextLine.endsWith("10:30"))
    }
}
