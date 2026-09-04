package com.travelcompanion.app.feature.walk

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.domain.walk.WalkModeState
import com.travelcompanion.app.domain.walk.WalkPhase
import com.travelcompanion.app.domain.walk.WalkStopState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Screen 11 — the walk closed, and the day handed back. */
class WalkFinishedStateTest {

    private val content = packagedContent(exists = { true })
    private val date = LocalDate.parse("2026-09-21")

    private fun epochAt(hour: Int, minute: Int): Long =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun completed(played: Set<String> = emptySet()) = WalkModeState(
        phase = WalkPhase.Completed,
        walkId = "walk.sarajevo.historical",
        walkTitle = "Caminhada Histórica de Sarajevo",
        stops = listOf(
            WalkStopState("story.meeting-of-cultures", "Onde Oriente e Ocidente se encontram", null, visited = true),
            WalkStopState("story.latin-bridge", "Latin Bridge", null, visited = true),
        ),
        playedStoryIds = played,
        startedAtEpochMs = epochAt(15, 30),
        completedAtEpochMs = epochAt(16, 12),
    )

    private fun state(
        walkState: WalkModeState = completed(),
        group: List<GroupParticipant> = emptyList(),
        time: LocalTime = LocalTime.of(16, 12),
    ) = buildWalkFinishedState(content, walkState, "vinicius", group, date, time)!!

    /** The walk's own clock, not the route's declared duration. */
    @Test
    fun `the duration is how long the walk actually ran`() {
        assertEquals("42 min", state().cards.first().value)
    }

    /**
     * The distance is the route's declared length: nothing in this app
     * measures how far anybody walked, and an odometer would be a subsystem.
     */
    @Test
    fun `the distance is the route the package declares`() {
        assertEquals("1,8 km", state().cards[1].value)
    }

    /**
     * Stories *heard*, which is not stories triggered: §20 keeps `playedAt`
     * separate from `triggeredAt` precisely so the two can disagree, and a
     * story whose audio is not packaged never counts.
     */
    @Test
    fun `stories heard counts the audio that really started`() {
        assertEquals("0 de 2", state().cards[2].value)
        assertEquals(
            "1 de 2",
            state(completed(played = setOf("story.latin-bridge"))).cards[2].value,
        )
    }

    /**
     * Who listened is this traveller plus what the app already holds about the
     * group. Opening screen 11 never asks the group anything (D071, D079).
     */
    @Test
    fun `who listened is the local traveller plus what is already known`() {
        assertEquals("Vinícius", state().cards[3].value)

        val withGroup = state(
            group = listOf(
                GroupParticipant("vinicius", ParticipantSync.Synchronized),
                GroupParticipant("erika", ParticipantSync.Synchronized),
            ),
        )
        assertEquals("Vinícius · Érika", withGroup.cards[3].value)
    }

    @Test
    fun `it says where and when the walk ended`() {
        assertEquals("Terminou em Sarajevo, às 16:12.", state().endedLine)
    }

    /** Finishing a walk at 16:12 is exactly when the 19:30 bus matters. */
    @Test
    fun `the day comes back, critical item and all`() {
        val finished = state()

        assertNotNull(finished.critical)
        assertEquals("19:30", finished.critical!!.nominalTime)
        assertTrue(finished.remaining.size <= 2)
        // The bus is the critical item and is drawn above in oxblood; it must
        // not also be one of the rows beneath.
        assertTrue(finished.remaining.none { "Ônibus" in it.item.title })
    }

    @Test
    fun `a walk that is not in the package is nothing to close`() {
        assertNull(
            buildWalkFinishedState(
                content,
                completed().copy(walkId = "does.not.exist"),
                "vinicius",
                emptyList(),
                date,
                LocalTime.NOON,
            ),
        )
    }
}
