package com.travelcompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelcompanion.app.data.preferences.ParticipantPreferences
import com.travelcompanion.app.data.trip.AssetTripRepository
import com.travelcompanion.app.design.FieldCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tripRepository = AssetTripRepository(applicationContext)
        val participantPreferences = ParticipantPreferences(applicationContext)

        setContent {
            FieldCompanionTheme {
                val rootViewModel: RootViewModel = viewModel(
                    factory = RootViewModel.factory(
                        tripRepository = tripRepository,
                        participantPreferences = participantPreferences,
                    ),
                )
                TravelCompanionRoot(rootViewModel)
            }
        }
    }
}
