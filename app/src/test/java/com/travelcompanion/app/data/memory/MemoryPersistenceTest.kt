package com.travelcompanion.app.data.memory

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * A memory outliving the process that recorded it.
 *
 * This is the reason Room is here at all (D057): a voice memory is the first
 * runtime fact the app has to be able to hand back after the process is gone,
 * and an in-memory database would prove nothing about that. So the database is
 * written to a real file, closed — which is as much as a test can do to end a
 * process — and opened again as a different instance.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MemoryPersistenceTest {

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val file = File(context.filesDir, "memories-test.db")

    private fun open(): MemoryDatabase =
        Room.databaseBuilder(context, MemoryDatabase::class.java, file.absolutePath).build()

    @After
    fun cleanUp() {
        file.delete()
    }

    @Test
    fun `a saved memory is still there after the database is reopened`() = runBlocking {
        val first = open()
        RoomMemoryRepository(first.memories()).save(
            Memory(
                id = "m1",
                fileName = "m1.m4a",
                recordedAtEpochMs = 1_700_000_000_000L,
                durationMs = 6_000L,
                participantId = "vinicius",
                participantName = "Vinícius",
                cityName = "Sarajevo",
                placeName = "Baščaršija",
            ),
        )
        first.close()

        val reopened = open()
        val rows = RoomMemoryRepository(reopened.memories()).memories().first()
        reopened.close()

        assertEquals(1, rows.size)
        assertEquals("m1", rows.single().id)
        assertEquals("Baščaršija", rows.single().placeName)
        assertEquals(6_000L, rows.single().durationMs)
    }

    /** Newest first, which is the order the list on screen 12 is read in. */
    @Test
    fun `memories come back newest first`() = runBlocking {
        val db = open()
        val memories = RoomMemoryRepository(db.memories())
        listOf(1_000L, 3_000L, 2_000L).forEachIndexed { index, at ->
            memories.save(
                Memory(
                    id = "m$index",
                    fileName = "m$index.m4a",
                    recordedAtEpochMs = at,
                    durationMs = 1_000L,
                    participantId = "vinicius",
                    participantName = "Vinícius",
                    cityName = null,
                    placeName = null,
                ),
            )
        }
        val rows = memories.memories().first()
        db.close()

        assertEquals(listOf(3_000L, 2_000L, 1_000L), rows.map { it.recordedAtEpochMs })
    }
}
