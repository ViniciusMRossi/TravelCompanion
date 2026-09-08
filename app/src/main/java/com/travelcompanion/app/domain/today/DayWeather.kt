package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripDay
import com.travelcompanion.app.data.weather.CachedWeather
import com.travelcompanion.app.data.weather.WeatherCache
import com.travelcompanion.app.data.weather.WeatherRepository
import com.travelcompanion.app.domain.explore.exploreCitiesOf
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** What one day needs before anything can be asked about its weather. */
data class DayWeatherRequest(
    val dayId: String,
    val timeZone: String,
    val point: GeoPoint,
)

/**
 * Where the day's weather is measured.
 *
 * **The city is `exploreCitiesOf(...).openAtCityId`, and deliberately not the
 * base.** "Where does this day happen" is one question with one answer, and
 * D152 already wrote it, ordered it and covered it with tests: the day's
 * itinerary decides, not the bed. Day 17 sleeps in Čilipi, which packages
 * nothing, and is spent inside Dubrovnik's walls — the forecast belongs to the
 * walls.
 *
 * **The coordinate is the centroid of that city's attractions**, and that is
 * the statistic, not a tiebreak. Weather is a regional quantity: two
 * attractions of Dubrovnik have the same forecast, and the mean of thirteen
 * points describes the city better than any one of them. Taking the first
 * element of a list because it is first is the unearned choice D097 names, and
 * it is avoided here rather than argued about.
 *
 * Cities carry no coordinates and accommodations carry none either; only
 * attractions do, and all forty-seven of them do. **Nineteen of the twenty
 * days yield a centroid.** The one that does not is day 1, São Paulo, whose
 * city packages no attraction at all: no coordinate, so no call, so the day
 * falls straight through to the packaged forecast — which is the right answer
 * for a day that starts at home and ends on a night flight.
 *
 * The mean is rounded to four decimals, about eleven metres. That is far finer
 * than any forecast grid, it keeps the request a stable string, and it lets
 * the cache compare coordinates exactly instead of approximately.
 */
fun dayWeatherRequestOf(content: TripContent, day: TripDay?): DayWeatherRequest? {
    if (day == null) return null
    val city = content.city(exploreCitiesOf(content, day).openAtCityId) ?: return null
    val points = city.attractionIds.mapNotNull { content.attraction(it)?.location?.geo }
    if (points.isEmpty()) return null
    return DayWeatherRequest(
        dayId = day.id,
        timeZone = day.timeZone,
        point = GeoPoint(
            latitude = roundToFourDecimals(points.sumOf { it.latitude } / points.size),
            longitude = roundToFourDecimals(points.sumOf { it.longitude } / points.size),
        ),
    )
}

private fun roundToFourDecimals(value: Double): Double = Math.round(value * 10_000.0) / 10_000.0

/**
 * The four states of brief §14, and the order they degrade in.
 *
 * `Live` → `Cached` → what the package already said. **The chain never steps
 * backwards**: a successful reading is written to the cache as it arrives, so
 * a failure that follows one shows that same reading with its own hour on it,
 * never the packaged forecast. Freshness is stated and never implied — a
 * cached reading says when it was taken, so nothing old can pass for new.
 *
 * A new day starts the chain again. The cache only answers for the day it was
 * taken on, because a reading is *the weather now at a place*, and yesterday's
 * "now" is worth less than the forecast the package carries for today.
 *
 * Nothing here blocks screen 02. The caller draws the packaged forecast first
 * and hands it in as [packaged]; whatever this returns is what the card
 * becomes *if* an answer arrives.
 */
class DayWeather(
    private val repository: WeatherRepository,
    private val cache: WeatherCache,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun stateFor(request: DayWeatherRequest?, packaged: WeatherUi): WeatherUi {
        if (request == null) return packaged

        val live = attempt { repository.forecast(request.point, request.timeZone) }
        if (live != null) {
            val readAt = now()
            attempt {
                cache.save(
                    CachedWeather(
                        dayId = request.dayId,
                        point = request.point,
                        reading = live,
                        readAtMillis = readAt,
                    ),
                )
            }
            return WeatherUi.Live(
                minC = live.minC,
                maxC = live.maxC,
                rainNote = rainNote(live.precipitationMm),
                readAtLabel = readAtLabel(readAt, request.timeZone),
            )
        }

        val cached = attempt { cache.last() }
            ?.takeIf { it.isAbout(request.dayId, request.point) }
            ?: return packaged

        return WeatherUi.Cached(
            minC = cached.reading.minC,
            maxC = cached.reading.maxC,
            rainNote = rainNote(cached.reading.precipitationMm),
            readAtLabel = readAtLabel(cached.readAtMillis, request.timeZone),
        )
    }

    /**
     * The hour the reading arrived, in the day's own zone.
     *
     * The date is added only when it is not the day being lived: on the road
     * the label is the four digits that matter, and a reading that survived
     * midnight says so instead of looking like it was taken minutes ago.
     */
    private fun readAtLabel(atMillis: Long, timeZone: String): String {
        val zone = runCatching { ZoneId.of(timeZone) }.getOrElse { ZoneId.systemDefault() }
        val read = Instant.ofEpochMilli(atMillis).atZone(zone).toLocalDateTime()
        val today = Instant.ofEpochMilli(now()).atZone(zone).toLocalDate()
        val hour = String.format(Locale.ROOT, "%02d:%02d", read.hour, read.minute)
        return if (read.toLocalDate() == today) {
            hour
        } else {
            String.format(Locale.ROOT, "%02d/%02d às %s", read.dayOfMonth, read.monthValue, hour)
        }
    }

    /**
     * A failure of any kind is *no answer*, never an exception that reaches
     * the screen. Cancellation is not a failure and is passed on: the job is
     * tied to screen 02, and swallowing it would keep working for a screen
     * that has gone.
     */
    private suspend fun <T> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failed: Exception) {
            null
        }

    private companion object {
        /** Portuguese, like every other string on this screen. */
        val PtBr: Locale = Locale.forLanguageTag("pt-BR")

        /** Millimetres, because that is what was measured. Below a tenth of one
         *  the honest reading is "none" rather than "0,0 mm". */
        fun rainNote(millimetres: Double?): String? = when {
            millimetres == null -> null
            millimetres < 0.05 -> "Sem chuva prevista."
            else -> "Chuva: ${String.format(PtBr, "%.1f", millimetres)} mm."
        }
    }
}
