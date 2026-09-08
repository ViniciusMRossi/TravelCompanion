package com.travelcompanion.app.data.weather

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.travelcompanion.app.data.trip.GeoPoint
import kotlinx.coroutines.flow.first

/**
 * The last successful reading, and the two facts that say whether it may be
 * shown.
 *
 * A forecast is only true of a place and of a moment, and the app stores both
 * with it. A reading taken in Kotor and drawn in Dubrovnik would be a wrong
 * forecast wearing the face of a right one — and it would *look* right almost
 * always, because consecutive days are usually spent in the same city. That is
 * the shape D089 names, so the cache carries what it needs to refuse itself.
 */
data class CachedWeather(
    val dayId: String,
    val point: GeoPoint,
    val reading: WeatherReading,
    val readAtMillis: Long,
) {
    /**
     * Whether this reading is about the day and the place being asked for.
     *
     * The coordinate is compared exactly because it is *made* exactly: the
     * centroid is rounded to four decimals before it is ever used, so the same
     * day always produces the same bits.
     */
    fun isAbout(dayId: String, point: GeoPoint): Boolean =
        this.dayId == dayId &&
            this.point.latitude == point.latitude &&
            this.point.longitude == point.longitude
}

/**
 * Brief §14, "store last successful result locally".
 *
 * One reading, not a history: the only reading worth keeping is the newest,
 * and it is only ever offered back for the day and place it was taken for.
 */
interface WeatherCache {
    suspend fun last(): CachedWeather?
    suspend fun save(weather: CachedWeather)
}

/**
 * DataStore Preferences, like every other runtime value of this size —
 * [com.travelcompanion.app.data.preferences.ParticipantPreferences] and
 * [com.travelcompanion.app.data.walk.DataStoreStoryTriggerStore]. Nothing
 * here is a table, so nothing here is Room.
 */
class DataStoreWeatherCache(
    private val dataStore: DataStore<Preferences>,
) : WeatherCache {

    override suspend fun last(): CachedWeather? {
        val prefs = dataStore.data.first()
        val dayId = prefs[DAY_ID] ?: return null
        val latitude = prefs[LATITUDE] ?: return null
        val longitude = prefs[LONGITUDE] ?: return null
        val readAt = prefs[READ_AT] ?: return null
        val minC = prefs[MIN_C]
        val maxC = prefs[MAX_C]
        if (minC == null && maxC == null) return null
        return CachedWeather(
            dayId = dayId,
            point = GeoPoint(latitude = latitude, longitude = longitude),
            reading = WeatherReading(minC = minC, maxC = maxC, precipitationMm = prefs[PRECIPITATION]),
            readAtMillis = readAt,
        )
    }

    override suspend fun save(weather: CachedWeather) {
        dataStore.edit { prefs ->
            prefs[DAY_ID] = weather.dayId
            prefs[LATITUDE] = weather.point.latitude
            prefs[LONGITUDE] = weather.point.longitude
            prefs[READ_AT] = weather.readAtMillis
            weather.reading.minC.let { if (it == null) prefs.remove(MIN_C) else prefs[MIN_C] = it }
            weather.reading.maxC.let { if (it == null) prefs.remove(MAX_C) else prefs[MAX_C] = it }
            weather.reading.precipitationMm
                .let { if (it == null) prefs.remove(PRECIPITATION) else prefs[PRECIPITATION] = it }
        }
    }

    private companion object {
        val DAY_ID = stringPreferencesKey("weather_day_id")
        val LATITUDE = doublePreferencesKey("weather_latitude")
        val LONGITUDE = doublePreferencesKey("weather_longitude")
        val READ_AT = longPreferencesKey("weather_read_at")
        val MIN_C = doublePreferencesKey("weather_min_c")
        val MAX_C = doublePreferencesKey("weather_max_c")
        val PRECIPITATION = doublePreferencesKey("weather_precipitation_mm")
    }
}
