package com.travelcompanion.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.domain.today.TodayUiState
import com.travelcompanion.app.domain.today.TodayUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

class TodayViewModel(
    content: TripContent,
    private val participantId: String?,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) : ViewModel() {

    private val today = TodayUseCase(content)

    private val _state = MutableStateFlow(compute())
    val state: StateFlow<TodayUiState?> = _state.asStateFlow()

    /** Today depends on the clock, so it is recomputed when the screen resumes. */
    fun refresh() {
        _state.value = compute()
    }

    private fun compute(): TodayUiState? {
        val now = clock()
        return today(participantId = participantId, date = now.toLocalDate(), time = now.toLocalTime())
    }

    companion object {
        fun factory(
            content: TripContent,
            participantId: String?,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(content, participantId) }
        }
    }
}
