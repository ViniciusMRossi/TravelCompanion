package com.travelcompanion.app.domain.today

import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TwoCityTrip
import com.travelcompanion.app.data.weather.CachedWeather
import com.travelcompanion.app.data.weather.WeatherCache
import com.travelcompanion.app.data.weather.WeatherReading
import com.travelcompanion.app.data.weather.WeatherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * The four states of brief §14 and the rules that move between them (D164).
 *
 * No test here touches a network: that is what the [WeatherRepository]
 * interface is for. The fakes answer, fail, or time out on demand.
 */
class DayWeatherTest {

    private val content: TripContent = TwoCityTrip.content()
    private val day = content.days.first { it.id == "day09" }

    /** What the package already says, and the floor every failure returns to. */
    private val packaged = WeatherUi.FallbackFromTrip(
        summary = "Clima esperado ameno.",
        minC = 13.0,
        maxC = 24.0,
        rainNote = "Levar uma camada leve.",
        windNote = null,
    )

    private val reading = WeatherReading(minC = 17.3, maxC = 24.1, precipitationMm = 0.0)

    /** 2026-09-21, 14:32 in Europe/Sarajevo — the zone day 9 is written in. */
    private val readAt = 1_789_993_920_000L

    // ---------------------------------------------------------------- the point

    /**
     * The centroid, and not the first attraction.
     *
     * Sarajevo packages Bascarsija at 43.8595 and the Latin Bridge at 43.8578;
     * the day's coordinate is neither of them, it is the mean. Reading element
     * zero would agree with this on a one-attraction city and disagree on
     * every other, which is the coincidence D097 refuses to build on.
     */
    @Test
    fun theCoordinateIsTheMeanOfTheCityAttractionsAndNotTheFirstOne() {
        val request = dayWeatherRequestOf(content, day)!!

        assertEquals("day09", request.dayId)
        assertEquals("Europe/Sarajevo", request.timeZone)
        assertEquals(43.85865, request.point.latitude, 0.0001)
        assertEquals(18.42995, request.point.longitude, 0.0001)

        val first = content.attraction(content.city("sarajevo")!!.attractionIds.first())!!
        assertNotEquals(first.location!!.geo!!.latitude, request.point.latitude)
    }

    /**
     * Day 1 of the real trip in miniature: Sao Paulo, an airport and a night
     * flight, and a city with no attraction to average.
     *
     * No coordinate means no call at all — not a call with a guessed one.
     */
    @Test
    fun aDayWhoseCityPackagesNoAttractionAsksNothing() {
        val emptyDay = day.copy(
            id = "day10",
            timeline = emptyList(),
            cityIds = listOf("mostar"),
            baseCityId = "mostar",
        )
        val repository = FakeWeatherRepository { reading }

        val request = dayWeatherRequestOf(content, emptyDay)
        assertNull("a city with no attraction yields no coordinate", request)

        val state = runBlocking { dayWeather(repository).stateFor(request, packaged) }

        assertSame("the packaged forecast is left exactly as it was", packaged, state)
        assertEquals("and nothing was asked of the provider", 0, repository.calls)
    }

    // --------------------------------------------------------------- the chain

    @Test
    fun aLiveReadingReplacesThePackagedForecastAndCarriesItsHour() {
        val state = runBlocking {
            dayWeather(FakeWeatherRepository { reading })
                .stateFor(dayWeatherRequestOf(content, day), packaged)
        } as WeatherUi.Live

        assertEquals(17.3, state.minC!!, 0.001)
        assertEquals(24.1, state.maxC!!, 0.001)
        assertEquals("Sem chuva prevista.", state.rainNote)
        assertEquals("14:32", state.readAtLabel)
    }

    /**
     * The chain never steps backwards.
     *
     * A failure after a live reading shows that reading — with its own hour on
     * it — and never falls all the way back to the package. It works because
     * the successful reading was written to the cache as it arrived, which is
     * the whole of the mechanism.
     */
    @Test
    fun aFailureAfterALiveReadingShowsThatReadingAndNotThePackage() {
        val cache = FakeWeatherCache()
        val request = dayWeatherRequestOf(content, day)

        val live = runBlocking {
            DayWeather(FakeWeatherRepository { reading }, cache) { readAt }
                .stateFor(request, packaged)
        }
        assertTrue("the first visit is live", live is WeatherUi.Live)

        // An hour later, with the radios off.
        val later = runBlocking {
            DayWeather(FakeWeatherRepository { null }, cache) { readAt + 3_600_000L }
                .stateFor(request, packaged)
        } as WeatherUi.Cached

        assertEquals(24.1, later.maxC!!, 0.001)
        assertEquals("the hour is the reading's, not now's", "14:32", later.readAtLabel)
    }

    /**
     * **A cache is refused when it is about somewhere else.**
     *
     * A forecast taken in Kotor and drawn in Dubrovnik is a wrong forecast
     * wearing a right one's face, and it would look right nearly always,
     * because consecutive days are usually spent in one city — the shape D089
     * names. The day id is deliberately the same in both halves, so the only
     * thing that can refuse the reading is the coordinate.
     */
    @Test
    fun aCachedReadingTakenSomewhereElseIsRefused() {
        val dubrovnik = DayWeatherRequest("day16", "Europe/Sarajevo", GeoPoint(42.64, 18.1109))
        val kotor = GeoPoint(42.4236, 18.7714)
        val cache = FakeWeatherCache(
            CachedWeather("day16", kotor, WeatherReading(19.0, 27.0, 0.0), readAt),
        )
        val offline = dayWeather(FakeWeatherRepository { null }, cache)

        val refused = runBlocking { offline.stateFor(dubrovnik, packaged) }

        assertSame("Kotor's forecast must not be drawn in Dubrovnik", packaged, refused)

        // And the same reading, taken where it is being asked about, is used.
        cache.stored =
            CachedWeather("day16", dubrovnik.point, WeatherReading(19.0, 27.0, 0.0), readAt)
        val accepted = runBlocking { offline.stateFor(dubrovnik, packaged) } as WeatherUi.Cached
        assertEquals(27.0, accepted.maxC!!, 0.001)
    }

    // -------------------------------------------------------------- the floor

    /**
     * Offline is the normal case: thirteen of the fifteen European nights have
     * no roaming. Both shapes of failure land on the packaged forecast and
     * neither of them reaches the screen.
     *
     * This guard is nearly green the day it is written — the packaged fallback
     * already worked, and that is the point. It is here so that the day
     * somebody makes the card wait for a network, or lets a provider throw
     * through it, *Today must remain functional* fails out loud in a build
     * rather than quietly on the road.
     */
    @Test
    fun neitherAnExceptionNorATimeoutDisturbsThePackagedForecast() {
        val request = dayWeatherRequestOf(content, day)

        listOf(IOException("no route to host"), SocketTimeoutException("read timed out"))
            .forEach { failure ->
                val state = runBlocking {
                    dayWeather(FakeWeatherRepository { throw failure }).stateFor(request, packaged)
                }
                assertSame("${failure::class.simpleName} must leave the card alone", packaged, state)
            }
    }

    @Test
    fun aProviderThatAnswersWithNothingIsAFailureLikeAnyOther() {
        val state = runBlocking {
            dayWeather(FakeWeatherRepository { null })
                .stateFor(dayWeatherRequestOf(content, day), packaged)
        }

        assertSame(packaged, state)
    }

    /** With no forecast packaged either, the floor is "no data at all". */
    @Test
    fun withNothingPackagedTheFloorIsUnavailable() {
        val state = runBlocking {
            dayWeather(FakeWeatherRepository { null })
                .stateFor(dayWeatherRequestOf(content, day), WeatherUi.Unavailable)
        }

        assertEquals(WeatherUi.Unavailable, state)
    }

    @Test
    fun rainIsReportedInTheMillimetresItWasMeasuredIn() {
        val state = runBlocking {
            dayWeather(FakeWeatherRepository { WeatherReading(11.0, 18.0, 2.4) })
                .stateFor(dayWeatherRequestOf(content, day), packaged)
        } as WeatherUi.Live

        assertEquals("Chuva: 2,4 mm.", state.rainNote)
    }

    private fun dayWeather(
        repository: WeatherRepository,
        cache: WeatherCache = FakeWeatherCache(),
    ) = DayWeather(repository, cache) { readAt }
}

private class FakeWeatherRepository(
    private val answer: () -> WeatherReading?,
) : WeatherRepository {
    var calls: Int = 0
        private set

    override suspend fun forecast(point: GeoPoint, timeZone: String): WeatherReading? {
        calls++
        return answer()
    }
}

private class FakeWeatherCache(var stored: CachedWeather? = null) : WeatherCache {
    override suspend fun last(): CachedWeather? = stored
    override suspend fun save(weather: CachedWeather) {
        stored = weather
    }
}
