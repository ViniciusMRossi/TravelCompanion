package com.travelcompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelcompanion.app.design.FieldCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Dependencies live on the application, not here: audioguide playback
        // must outlive this Activity being recreated.
        val container = appContainer

        setContent {
            FieldCompanionTheme {
                val rootViewModel: RootViewModel = viewModel(
                    factory = RootViewModel.factory(
                        tripRepository = container.tripRepository,
                        participantPreferences = container.participantPreferences,
                    ),
                )
                TravelCompanionRoot(
                    viewModel = rootViewModel,
                    playbackController = container.playbackController,
                    walkModeController = container.walkModeController,
                )
            }
        }
    }
}
