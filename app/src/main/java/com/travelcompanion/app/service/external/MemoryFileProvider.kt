package com.travelcompanion.app.service.external

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider

/**
 * The `FileProvider` for voice memories, with a name a person can read.
 *
 * A memory's file is named by its id, because brief §22 says a recording must
 * be findable on disk without the app. The share sheet shows whatever the
 * provider calls the file, so without this the traveller is offered
 * "ec5f6817-63b2-…-8895a7d.m4a" — a UUID, in the one place where the handoff
 * asks for the memory's own name.
 *
 * The name travels as a query parameter on the URI and is substituted here.
 * Nothing else changes: the paths this provider exposes are still
 * `files/memories/` and nothing more (D082).
 */
class MemoryFileProvider : FileProvider(com.travelcompanion.app.R.xml.memory_file_paths) {

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val underlying = super.query(uri, projection, selection, selectionArgs, sortOrder)
        val displayName = uri.getQueryParameter(DISPLAY_NAME) ?: return underlying

        val columns = underlying.columnNames
        val row = arrayOfNulls<Any>(columns.size)
        underlying.use { cursor ->
            if (!cursor.moveToFirst()) return cursor
            columns.forEachIndexed { index, column ->
                row[index] = when (column) {
                    OpenableColumns.DISPLAY_NAME -> displayName
                    OpenableColumns.SIZE -> cursor.getLong(index)
                    else -> cursor.getString(index)
                }
            }
        }
        return MatrixCursor(columns, 1).apply { addRow(row) }
    }

    companion object {
        const val DISPLAY_NAME = "displayName"
    }
}
