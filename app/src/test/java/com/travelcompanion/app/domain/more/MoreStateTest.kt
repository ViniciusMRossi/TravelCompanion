package com.travelcompanion.app.domain.more

import com.travelcompanion.app.data.sync.GroupParticipant
import com.travelcompanion.app.data.sync.ParticipantSync
import com.travelcompanion.app.data.trip.AssetResolver
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.trip.TripContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Screen 19 — Mais. */
class MoreStateTest {

    private val nothingPackaged = packagedContent(exists = { false })

    /**
     * Opening "Mais" does not join the group, so with screen 09 unopened there
     * is no state to show and the section says offline rather than pretending
     * (D071). The sentence beneath is what carries the meaning.
     */
    @Test
    fun `with nothing known the group reads offline`() {
        val state = buildMoreState(nothingPackaged, known = emptyList(), localParticipantId = "vinicius")

        assertEquals(listOf("Offline", "Offline"), state.members.map { it.label })
        assertTrue(state.members.all { it.sync == null })
        assertTrue(state.groupNote.contains("se reencontra sozinho"))
        assertTrue(state.groupNote.contains("sem que nada pare de funcionar"))
    }

    @Test
    fun `a state the app already holds is the state it shows`() {
        val state = buildMoreState(
            nothingPackaged,
            known = listOf(
                GroupParticipant("vinicius", ParticipantSync.Synchronized),
                GroupParticipant("erika", ParticipantSync.Reconnecting),
            ),
            localParticipantId = "vinicius",
        )

        assertEquals(listOf("Sincronizado", "Sincronizando novamente"), state.members.map { it.label })
        assertTrue(state.members.first().name.endsWith("· você"))
    }

    /** No network vocabulary reaches this screen. */
    @Test
    fun `the group section says nothing about infrastructure`() {
        val state = buildMoreState(nothingPackaged, emptyList(), "vinicius")
        val words = listOf("Firebase", "servidor", "conexão", "rede", "sincronização", "socket")

        val text = state.groupNote + state.members.joinToString(" ") { it.label }
        words.forEach { word -> assertTrue(word, !text.contains(word, ignoreCase = true)) }
    }

    /**
     * The same rule as the wallet's badge (D013): only files this build really
     * carries are counted, so the size is not a promise about content that is
     * not there.
     */
    @Test
    fun `saved content counts only what is really on the device`() {
        assertEquals(emptyList<SavedContentUi>(), buildMoreState(nothingPackaged, emptyList(), null).savedContent)

        // Every declared file present, each a megabyte.
        val everything = TripContent(
            nothingPackaged.trip,
            AssetResolver(nothingPackaged.trip.assets, exists = { true }, sizeOf = { 1_000_000 }),
        )
        val state = buildMoreState(everything, emptyList(), null)

        assertTrue(state.savedContent.isNotEmpty())
        assertTrue(state.savedContent.all { it.count > 0 })
        assertEquals(
            formatBytes(state.savedContent.sumOf { it.bytes }),
            state.savedTotalLabel,
        )
    }

    @Test
    fun `every Plan B of the trip is listed, not just this day's`() {
        val state = buildMoreState(nothingPackaged, emptyList(), null)
        assertEquals(listOf("planb.day09", "planb.bascarsija.rain"), state.planBs.map { it.id })
    }

    @Test
    fun `sizes are read the way a person reads them`() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("12 kB", formatBytes(12_300))
        assertEquals("3,4 MB", formatBytes(3_400_000))
    }

    @Test
    fun `the trip section names the trip and its window`() {
        val state = buildMoreState(nothingPackaged, emptyList(), null)
        assertEquals("2026-09-13 — 2026-10-03", state.tripDates)
        assertTrue(state.tripTitle.isNotBlank())
        assertNull(state.members.firstOrNull { it.name.contains("null") })
    }
}
