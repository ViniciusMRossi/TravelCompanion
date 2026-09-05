package com.travelcompanion.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelcompanion.app.design.FieldCompanionTheme
import com.travelcompanion.app.service.notification.OperationalNotifications

class MainActivity : ComponentActivity() {

    /**
     * The screen a notification asked for, held until the graph exists.
     *
     * `SINGLE_TOP` means a tap on a second deadline arrives at the Activity
     * that is already running, through [onNewIntent] rather than [onCreate];
     * both write here so both open the right screen (D092).
     */
    private var requestedRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedRoute = intent?.getStringExtra(OperationalNotifications.EXTRA_ROUTE)
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
                    criticalAlertScheduler = container.criticalAlertScheduler,
                    requestedRoute = requestedRoute,
                    onRouteHandled = { requestedRoute = null },
                    playbackController = container.playbackController,
                    walkModeController = container.walkModeController,
                    groupSessionController = container::groupSessionController,
                    memoryController = container.memoryController,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedRoute = intent.getStringExtra(OperationalNotifications.EXTRA_ROUTE)
    }
}
