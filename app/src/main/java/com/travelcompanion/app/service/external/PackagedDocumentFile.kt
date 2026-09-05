package com.travelcompanion.app.service.external

import android.content.Context
import java.io.File

/**
 * Puts a packaged document somewhere a viewer can open it.
 *
 * The files live in the APK's assets, and `assets/` has no path a
 * `FileProvider` can hand out — an asset is not a file on disk. So the one
 * document the traveller asked for is copied into a cache directory of its
 * own, and that is the only thing the provider exposes (D091).
 *
 * Cache, deliberately: the original is in the APK and can be written again at
 * any time, so the copy is disposable and the system may reclaim it. Nothing
 * here is a second source of truth.
 */
class PackagedDocumentFile(private val context: Context) {

    /**
     * A readable copy of [assetPath], named [displayName] so the viewer's
     * title bar reads like a document rather than like a build artefact.
     *
     * Null when the asset is not in this build — the same absence
     * `DocumentAccess.NotPackaged` already describes, surfaced late.
     */
    fun materialise(assetPath: String, displayName: String): File? = runCatching {
        val target = File(directory(), displayName)
        context.assets.open(assetPath).use { input ->
            target.outputStream().use(input::copyTo)
        }
        target
    }.getOrNull()

    private fun directory(): File =
        File(context.cacheDir, DIRECTORY).apply {
            // A stale copy of a document that has since changed would be a
            // wrong ticket at a counter, so the directory holds one file.
            if (isDirectory) listFiles()?.forEach(File::delete) else mkdirs()
        }

    companion object {
        const val DIRECTORY = "documents"
    }
}
