package com.travelcompanion.app.data.weather

import com.travelcompanion.app.data.trip.GeoPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * One day's forecast at one point, as a provider gave it.
 *
 * Numbers only. Every sentence the card draws is written by [
 * com.travelcompanion.app.domain.today.DayWeather], so a provider cannot put
 * text on screen 02.
 */
data class WeatherReading(
    val minC: Double?,
    val maxC: Double?,
    val precipitationMm: Double?,
)

/**
 * Live weather, brief §14.
 *
 * The interface exists so nothing else in the app knows there is a network at
 * all — and so no test needs one. `null` means *no answer*: an implementation
 * reports failure by returning nothing rather than by throwing, and
 * [com.travelcompanion.app.domain.today.DayWeather] guards against throwing
 * anyway, because Today must remain functional whatever a provider does.
 */
interface WeatherRepository {
    suspend fun forecast(point: GeoPoint, timeZone: String): WeatherReading?
}

/**
 * Open-Meteo, and no dependency (D164).
 *
 * The provider was chosen for one property above accuracy: **no key and no
 * registration** for non-commercial use. A key would have to live either in a
 * versioned file — which this repository will not carry — or in a
 * `local.properties` outside git, which is one more thing to keep working five
 * days before departure.
 *
 * `HttpURLConnection` and `kotlinx.serialization`, both already on the
 * classpath. The timeouts are short on purpose: thirteen of the fifteen
 * European nights have no roaming, so failing is the normal case and it must
 * fail *fast and quietly*. There is no retry, no back-off and no scheduler —
 * one attempt per visit to the screen, and the packaged forecast is already on
 * screen while it happens.
 *
 * Nothing here is logged. The coordinate is where the traveller is going to be.
 */
class OpenMeteoWeatherRepository(
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : WeatherRepository {

    override suspend fun forecast(point: GeoPoint, timeZone: String): WeatherReading? =
        withContext(io) {
            runCatching { fetch(openMeteoUrl(point, timeZone)) }.getOrNull()
        }

    private fun fetch(url: String): WeatherReading? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
        }
        val body = try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        return json.decodeFromString<OpenMeteoForecast>(body).firstDay()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 4_000
        const val READ_TIMEOUT_MS = 4_000

        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * The request, spelled out so it can be asserted without a network.
 *
 * `forecast_days=1` asks for one day at that place: the day the *provider* is
 * living, which during the trip is the day screen 02 is drawing. The zone is
 * the day's own, so a maximum belongs to the local calendar day rather than to
 * UTC's.
 */
internal fun openMeteoUrl(point: GeoPoint, timeZone: String): String =
    "https://api.open-meteo.com/v1/forecast" +
        "?latitude=${point.latitude}" +
        "&longitude=${point.longitude}" +
        "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum" +
        "&timezone=${URLEncoder.encode(timeZone, "UTF-8")}" +
        "&forecast_days=1"

@Serializable
private data class OpenMeteoForecast(
    val daily: OpenMeteoDaily? = null,
) {
    /** Null rather than a half-filled reading: a response with no temperature
     *  in it is not a forecast, and must degrade like a failure. */
    fun firstDay(): WeatherReading? {
        val daily = daily ?: return null
        val minC = daily.minC.firstOrNull()
        val maxC = daily.maxC.firstOrNull()
        if (minC == null && maxC == null) return null
        return WeatherReading(
            minC = minC,
            maxC = maxC,
            precipitationMm = daily.precipitationMm.firstOrNull(),
        )
    }
}

@Serializable
private data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val maxC: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val minC: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationMm: List<Double?> = emptyList(),
)
