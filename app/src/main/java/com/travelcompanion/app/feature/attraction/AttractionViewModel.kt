package com.travelcompanion.app.feature.attraction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.service.playback.AudioGuideRequest
import com.travelcompanion.app.service.playback.PlaybackController
import com.travelcompanion.app.service.playback.PlaybackState
import com.travelcompanion.app.service.playback.audioGuideRequest
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * Screen 05's binding to content and to playback.
 *
 * It holds no player: [PlaybackController] lives on the application, so
 * leaving this screen — or rotating it — does not interrupt the audioguide.
 * That is why [onCleared] deliberately does nothing.
 */
class AttractionViewModel(
    private val content: TripContent,
    attractionId: String,
    private val playback: PlaybackController,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    val state: AttractionUiState? = buildAttractionState(content, attractionId, today)

    val playbackState: StateFlow<PlaybackState> = playback.state

    /** The audioguide this screen controls, if the attraction has one. */
    private val request: AudioGuideRequest? =
        audioGuideRequest(
            content = content,
            audioGuideId = content.attraction(attractionId)?.audioGuideId,
            subtitle = state?.name,
        )

    val audioGuideMediaId: String? = request?.mediaId

    fun onPlayAudioGuide() {
        val request = request ?: return
        playback.playAudioGuide(request)
    }

    fun onTogglePlayPause() = playback.togglePlayPause()

    fun onSkipBack() = playback.seekBy(-SKIP_MS)

    fun onSkipForward() = playback.seekBy(SKIP_MS)

    companion object {
        private const val SKIP_MS = 15_000L

        fun factory(
            content: TripContent,
            attractionId: String,
            playback: PlaybackController,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { AttractionViewModel(content, attractionId, playback) }
        }
    }
}
