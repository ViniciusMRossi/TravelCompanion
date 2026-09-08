package com.travelcompanion.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.today.DayWeather
import com.travelcompanion.app.domain.today.TodayUiState
import com.travelcompanion.app.domain.today.TodayUseCase
import com.travelcompanion.app.domain.today.dayWeatherRequestOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TodayViewModel(
    private val content: TripContent,
    private val participantId: String?,
    /** Null leaves the card on the packaged forecast, which is what previews want. */
    private val weather: DayWeather? = null,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) : ViewModel() {

    private val today = TodayUseCase(content)

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<TodayUiState?> = _state.asStateFlow()

    private var weatherJob: Job? = null

    /** Today depends on the clock, so it is recomputed when the screen resumes. */
    fun refresh() {
        _state.value = compute()
        askForWeather()
    }

    /**
     * The one place in this app that touches a network, and it touches it
     * *beside* the screen rather than in front of it.
     *
     * [_state] already holds the packaged forecast by the time this starts, so
     * the card is drawn and complete whether or not an answer ever arrives.
     * One attempt per resume: no loop, no polling, no scheduler. The attempt
     * is a `viewModelScope` job so leaving screen 02 cancels it, and a second
     * resume replaces the first rather than racing it (D164).
     */
    private fun askForWeather() {
        val weather = weather ?: return
        val day = content.dayFor(clock().toLocalDate()) ?: return
        val request = dayWeatherRequestOf(content, day) ?: return

        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            val started = _state.value ?: return@launch
            val resolved = weather.stateFor(request, started.weather)
            _state.update { current ->
                // The clock may have moved the day on while the answer was in
                // flight; a forecast belonging to a day that is no longer on
                // screen is dropped rather than drawn (D089).
                if (current != null && current.dateLabel == started.dateLabel) {
                    current.copy(weather = resolved)
                } else {
                    current
                }
            }
        }
    }

    private fun compute(): TodayUiState? {
        val now = clock()
        return today(participantId = participantId, date = now.toLocalDate(), time = now.toLocalTime())
    }

    companion object {
        fun factory(
            content: TripContent,
            participantId: String?,
            weather: DayWeather?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(content, participantId, weather) }
        }
    }
}
