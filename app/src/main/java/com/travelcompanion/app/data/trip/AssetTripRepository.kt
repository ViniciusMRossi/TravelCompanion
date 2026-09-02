package com.travelcompanion.app.data.trip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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

    private fun buildContent(): TripContent {
        val trip = assets.open(TRIP_ASSET_PATH)
            .bufferedReader()
            .use { reader -> json.decodeFromString<TripPackage>(reader.readText()) }

        return TripContent(
            trip = trip,
            assets = AssetResolver(trip.assets, exists = ::packagedFileExists),
        )
    }

    /**
     * Photography and audio are produced after the app; a missing binary must
     * degrade to the approved placeholder rather than crash a screen.
     */
    private fun packagedFileExists(path: String): Boolean = runCatching {
        assets.open(path).close()
        true
    }.getOrDefault(false)

    private companion object {
        const val TRIP_ASSET_PATH = "trip/trip.json"
    }
}
