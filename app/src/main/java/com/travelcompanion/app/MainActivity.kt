package com.travelcompanion.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelcompanion.app.design.FieldCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light bars, always. The bare `enableEdgeToEdge()` picks the system
        // bar icons from the system's night setting, and this app has no dark
        // palette: with night mode on, white icons landed on the paper strip
        // above every screen and the clock became unreadable. The theme
        // already declares `windowLightStatusBar`; this is the same intent
        // said where edge-to-edge can hear it (D052).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

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
                    groupSessionController = container::groupSessionController,
                    memoryController = container.memoryController,
                )
            }
        }
    }
}
