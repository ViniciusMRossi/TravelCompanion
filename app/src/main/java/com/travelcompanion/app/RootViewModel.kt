package com.travelcompanion.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.travelcompanion.app.data.preferences.ParticipantPreferences
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.data.trip.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RootUiState(
    val loading: Boolean = true,
    val content: TripContent? = null,
    val participantId: String? = null,
    val error: String? = null,
)

class RootViewModel(
    private val tripRepository: TripRepository,
    private val participantPreferences: ParticipantPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(RootUiState())
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { tripRepository.load() }
                .onSuccess { content ->
                    _state.update { it.copy(loading = false, content = content) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Não foi possível carregar a viagem.",
                        )
                    }
                }
        }

        viewModelScope.launch {
            participantPreferences.participantId.collectLatest { id ->
                _state.update { it.copy(participantId = id) }
            }
        }
    }

    fun selectParticipant(id: String) {
        viewModelScope.launch { participantPreferences.setParticipant(id) }
    }

    fun resetParticipant() {
        viewModelScope.launch { participantPreferences.clearParticipant() }
    }

    companion object {
        fun factory(
            tripRepository: TripRepository,
            participantPreferences: ParticipantPreferences,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RootViewModel(tripRepository, participantPreferences) }
        }
    }
}
