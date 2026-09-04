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
        return renameIn(underlying, displayName)
    }

    companion object {
        const val DISPLAY_NAME = "displayName"
    }
}

/**
 * The row this class actually adds, apart from the provider it lives in.
 *
 * This is the only cursor assembled by hand in the repository and the only
 * code another application reads through, so it is a function that can be
 * driven directly rather than something reachable only through a registered
 * provider and a path strategy (D084).
 *
 * Two things it is careful about. The row is read out and [underlying] closed
 * *before* anything is returned, so an empty answer is an empty cursor that is
 * still open — returning the source cursor from inside `use` would hand the
 * caller a closed one, and the first call on it throws. And every column but
 * the name is copied by the cursor's own declared type, so the copy stays
 * correct whatever columns the superclass turns out to emit.
 */
internal fun renameIn(underlying: Cursor, displayName: String): Cursor {
    val columns = underlying.columnNames
    val row: Array<Any?>? = underlying.use { cursor ->
        if (!cursor.moveToFirst()) {
            null
        } else {
            Array(columns.size) { index ->
                when {
                    columns[index] == OpenableColumns.DISPLAY_NAME -> displayName
                    cursor.isNull(index) -> null
                    cursor.getType(index) == Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                    cursor.getType(index) == Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                    cursor.getType(index) == Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(index)
                    else -> cursor.getString(index)
                }
            }
        }
    }

    return MatrixCursor(columns, if (row == null) 0 else 1).apply { row?.let(::addRow) }
}
