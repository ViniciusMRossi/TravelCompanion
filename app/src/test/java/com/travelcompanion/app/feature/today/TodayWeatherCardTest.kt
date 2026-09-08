package com.travelcompanion.app.feature.today

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import com.travelcompanion.app.data.trip.GeoPoint
import com.travelcompanion.app.data.trip.PackagedTripTest.Companion.packagedContent
import com.travelcompanion.app.data.weather.CachedWeather
import com.travelcompanion.app.data.weather.WeatherCache
import com.travelcompanion.app.data.weather.WeatherReading
import com.travelcompanion.app.data.weather.WeatherRepository
import com.travelcompanion.app.domain.today.DayWeather
import com.travelcompanion.app.domain.today.TodayUiState
import com.travelcompanion.app.domain.today.TodayUseCase
import com.travelcompanion.app.domain.today.WeatherUi
import com.travelcompanion.app.domain.today.dayWeatherRequestOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.LocalTime

/**
 * The weather card of screen 02, in each of the four states of brief §14.
 *
 * Two of the four did not exist until D164, and the property that matters is
 * not that they draw a number: it is that **each one says where its number came
 * from**. A cached forecast drawn without its hour is a stale reading wearing a
 * live one's face, and there is no way to see that in a state assertion — the
 * data class is identical either way. So the card is composed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class TodayWeatherCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val content = packagedContent()
    private val date: LocalDate = LocalDate.parse("2026-09-21")

    private fun todayState(): TodayUiState =
        TodayUseCase(content)("vinicius", date, LocalTime.of(12, 0))!!

    private fun screen(state: TodayUiState) {
        compose.setContent {
            TodayScreen(
                state = state,
                onOpenAttraction = {},
                onOpenMaps = {},
                onOpenTimelineItem = {},
                onOpenFullDay = {},
                onOpenShortcut = {},
                onOpenAlarmSettings = {},
            )
        }
    }

    /** Scrolled to, because screen 02 is taller than any phone. */
    private fun seeing(text: String) = compose.onAllNodesWithText(text, substring = true)
        .onFirst().performScrollTo().assertIsDisplayed()

    /** The header does not scroll, so it is asserted where it stands. */
    private fun seeingInHeader(text: String) =
        compose.onAllNodesWithText(text, substring = true).onFirst().assertIsDisplayed()

    private fun notSeeing(text: String) =
        compose.onAllNodesWithText(text, substring = true).assertCountEquals(0)

    /** A live reading is labelled live, and carries the hour it arrived. */
    @Test
    fun `a live reading says it is live and says when`() {
        screen(
            todayState().copy(
                weather = WeatherUi.Live(
                    minC = 17.3,
                    maxC = 24.1,
                    rainNote = "Sem chuva prevista.",
                    readAtLabel = "14:32",
                ),
            ),
        )

        seeing("Ao vivo · 14:32")
        seeing("17° / 24°")
        seeing("Sem chuva prevista.")
        notSeeing("Última leitura")
        notSeeing("previsão salva na viagem")
    }

    /**
     * The one that matters most.
     *
     * A cached reading must never be able to pass for a live one, so the hour
     * it was taken is on the card and the word "live" is not.
     */
    @Test
    fun `a cached reading says when it was taken and is never called live`() {
        screen(
            todayState().copy(
                weather = WeatherUi.Cached(
                    minC = 17.3,
                    maxC = 24.1,
                    rainNote = "Chuva: 2,4 mm.",
                    readAtLabel = "14:32",
                ),
            ),
        )

        seeing("Última leitura · 14:32")
        seeing("Chuva: 2,4 mm.")
        notSeeing("Ao vivo ·")
    }

    /** The packaged forecast says it is packaged, exactly as it always did. */
    @Test
    fun `the packaged forecast says it is packaged`() {
        val state = todayState()
        assertEquals(
            "the packaged day still resolves to its trip fallback",
            WeatherUi.FallbackFromTrip::class,
            state.weather::class,
        )

        screen(state)

        seeing("Sem dados ao vivo · previsão salva na viagem")
        notSeeing("Ao vivo ·")
        notSeeing("Última leitura")
    }

    /** And a day with no forecast at all still says so in a sentence. */
    @Test
    fun `no forecast at all is a sentence, not an empty card`() {
        screen(todayState().copy(weather = WeatherUi.Unavailable))

        seeing("Sem previsão salva para este dia.")
        notSeeing("Ao vivo ·")
        notSeeing("Última leitura")
    }

    /**
     * **Today must remain functional** — the invariant, as a regression guard.
     *
     * With the provider failing both ways a provider fails, the card keeps the
     * day's packaged forecast and the whole of screen 02 draws: the now block,
     * the critical deadline, the day's timeline, the shortcuts, the offline
     * chip. Nothing waits for a network and nothing else on the screen moves.
     *
     * It is nearly green the day it is written, and that is the point: the
     * packaged path already worked, and this is what fails out loud the day
     * somebody makes the card block on an answer instead of beside it.
     */
    @Test
    fun `with the provider failing, the whole of screen 02 still draws`() {
        val state = todayState()
        val request = dayWeatherRequestOf(content, content.days.single())

        listOf(IOException("no route to host"), SocketTimeoutException("read timed out"))
            .forEach { failure ->
                val resolved = runBlocking {
                    DayWeather(
                        repository = FailingRepository(failure),
                        cache = EmptyCache,
                    ).stateFor(request, state.weather)
                }
                assertSame(
                    "${failure::class.simpleName} must leave the packaged forecast in place",
                    state.weather,
                    resolved,
                )
            }

        screen(state)

        seeing("Sem dados ao vivo · previsão salva na viagem")
        seeing("Baščaršija")
        seeing("O dia inteiro")
        seeingInHeader("Offline · conteúdo disponível")
    }
}

private class FailingRepository(private val failure: Exception) : WeatherRepository {
    override suspend fun forecast(point: GeoPoint, timeZone: String): WeatherReading? =
        throw failure
}

private object EmptyCache : WeatherCache {
    override suspend fun last(): CachedWeather? = null
    override suspend fun save(weather: CachedWeather) = Unit
}
