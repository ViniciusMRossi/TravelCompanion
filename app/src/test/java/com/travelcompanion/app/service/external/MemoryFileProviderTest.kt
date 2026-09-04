package com.travelcompanion.app.service.external

import android.database.Cursor
import android.database.MatrixCursor
import android.provider.OpenableColumns
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The row the memory provider hands to other applications.
 *
 * This is the only surface of this app another application talks to, and the
 * only cursor in the repository assembled by hand — so it is the row that is
 * exercised here, driven directly. What sits above it is AndroidX's
 * `FileProvider`, which needs no test of ours; what sits below it is a path
 * strategy that resolves by canonical path, which under Robolectric on Windows
 * disagrees with its own temporary data directory (D084).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MemoryFileProviderTest {

    private fun fileRow(name: String, size: Long): Cursor =
        MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), 1).apply {
            addRow(arrayOf<Any>(name, size))
        }

    /**
     * The reason this class exists instead of the stock `FileProvider`: the
     * file on disk is named by the memory's id, and a UUID is not what anyone
     * wants to be offered in a share sheet.
     */
    @Test
    fun `the name asked for replaces the file's own`() {
        val renamed = renameIn(
            fileRow("ec5f6817-63b2-4378-b7e8-749948895a7d.m4a", 83_302L),
            "Sarajevo.m4a",
        )

        renamed.use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(
                "Sarajevo.m4a",
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)),
            )
        }
    }

    /** Everything else in the row survives the substitution untouched. */
    @Test
    fun `the size is carried over as a number`() {
        renameIn(fileRow("m1.m4a", 83_302L), "Ponte Latina.m4a").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val index = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
            assertEquals(Cursor.FIELD_TYPE_INTEGER, cursor.getType(index))
            assertEquals(83_302L, cursor.getLong(index))
        }
    }

    /** A caller that asked for one column gets one column back. */
    @Test
    fun `the columns are the ones that came in`() {
        val single = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME), 1).apply {
            addRow(arrayOf<Any>("m2.m4a"))
        }

        renameIn(single, "Ksamil.m4a").use { cursor ->
            assertArrayEquals(arrayOf(OpenableColumns.DISPLAY_NAME), cursor.columnNames)
            assertTrue(cursor.moveToFirst())
            assertEquals("Ksamil.m4a", cursor.getString(0))
        }
    }

    /**
     * The branch that was wrong.
     *
     * `use` is inline, so returning the source cursor from inside it closed
     * that cursor before the caller ever saw it: the first call on it would
     * have thrown "attempt to re-open an already-closed object". Not reachable
     * through the real superclass, which always emits exactly one row — which
     * is precisely why the branch had to be made correct rather than trusted.
     */
    @Test
    fun `an empty answer is an empty cursor that is still open`() {
        val nothing = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), 0)

        val renamed = renameIn(nothing, "Sarajevo.m4a")

        assertFalse("the caller can still read it", renamed.isClosed)
        assertEquals(0, renamed.count)
        assertFalse(renamed.moveToFirst())
        renamed.close()
    }

    /**
     * And the source is closed, whichever way it went: this row is copied out
     * of it, never a view onto it.
     */
    @Test
    fun `the cursor it was given is closed`() {
        val source = fileRow("m3.m4a", 10L)

        renameIn(source, "Latin Bridge.m4a").close()

        assertTrue(source.isClosed)
    }

    /**
     * Columns are copied by the type the cursor declares, not by a guess about
     * which columns there will be — the superclass emits two today and this
     * does not depend on that staying true.
     */
    @Test
    fun `a column of any type survives, including a null one`() {
        val wider = MatrixCursor(
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, "note", "ratio"),
            1,
        ).apply { addRow(listOf(null, 12L, null, 0.5)) }

        renameIn(wider, "Ksamil, última manhã.m4a").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ksamil, última manhã.m4a", cursor.getString(0))
            assertEquals(12L, cursor.getLong(1))
            assertNull(cursor.getString(2))
            assertEquals(0.5, cursor.getDouble(3), 0.0001)
        }
    }
}
