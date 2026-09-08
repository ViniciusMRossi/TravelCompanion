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

    /**
     * The audioguide this screen controls, if the attraction has one.
     *
     * The second line says *where*, not *what*: the title already carries the
     * name of the place, so repeating it there told the traveller nothing the
     * first line had not (D151). One rule for all forty-seven — the seven
     * whose guide happens to be titled differently do not get a branch of
     * their own, because a conditional whose arm turns on two strings being
     * equal is the shape D089 warned about.
     */
    private val request: AudioGuideRequest? =
        audioGuideRequest(
            content = content,
            audioGuideId = content.attraction(attractionId)?.audioGuideId,
            subtitle = state?.cityLine?.takeIf { it.isNotBlank() },
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
