package com.travelcompanion.app.data.trip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

class AssetTripRepository(
    context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : TripRepository {

    private val assets = context.applicationContext.assets

    @Volatile
    private var cached: TripContent? = null

    override suspend fun load(): TripContent {
        cached?.let { return it }

        return withContext(Dispatchers.IO) {
            cached ?: buildContent().also { cached = it }
        }
    }

    private fun buildContent(): TripContent =
        buildContent(json, ::openPackagedFile, ::packagedFileExists, ::packagedFileSize)

    private fun openPackagedFile(path: String): InputStream = assets.open(path)

    /**
     * Photography and audio are produced after the app; a missing binary must
     * degrade to the approved placeholder rather than crash a screen.
     */
    private fun packagedFileExists(path: String): Boolean = runCatching {
        assets.open(path).close()
        true
    }.getOrDefault(false)

    /**
     * What the file occupies inside the APK, as the asset stream reports it.
     * Null rather than zero when it cannot be read, so the screen can stay
     * quiet instead of claiming a size of nothing.
     */
    private fun packagedFileSize(path: String): Long? = runCatching {
        assets.open(path).use { it.available().toLong() }
    }.getOrNull()
}

/**
 * Reads the packaged trip out of whichever root this build carries.
 *
 * Free of [Context] so both roots can be driven in a unit test — the branch it
 * takes decides where every screen's content and every document's file come
 * from, and it is not observable from the outside once the app is running.
 *
 * The root is resolved **once**, and the same value both names the `trip.json`
 * to open and goes to the [AssetResolver]. That is the whole point: the
 * content and its assets cannot come from different directories (D087).
 */
internal fun buildContent(
    json: Json,
    open: (String) -> InputStream,
    exists: (String) -> Boolean,
    sizeOf: (String) -> Long?,
): TripContent {
    val root = TripAssetRoot.resolve(exists)

    val trip = open(root.tripJsonPath)
        .bufferedReader()
        .use { reader -> json.decodeFromString<TripPackage>(reader.readText()) }

    return TripContent(
        trip = trip,
        assets = AssetResolver(trip.assets, root, exists, sizeOf),
    )
}
