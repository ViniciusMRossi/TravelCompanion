package com.travelcompanion.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.feature.shell.AppNavigation
import com.travelcompanion.app.feature.whoareyou.WhoAreYouScreen

@Composable
fun TravelCompanionRoot(viewModel: RootViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = state.content

    when {
        state.loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FieldCompanionColors.Paper),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = FieldCompanionColors.Teal)
            }
        }

        state.error != null || content == null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FieldCompanionColors.Paper)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = "Não foi possível abrir o conteúdo da viagem.",
                    style = TcType.subheadline,
                    color = FieldCompanionColors.Ink,
                )
                Text(
                    text = state.error.orEmpty(),
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
        }

        // First launch asks only "Quem é você?" — never a login.
        state.participantId == null -> {
            WhoAreYouScreen(
                content = content,
                onParticipantSelected = viewModel::selectParticipant,
            )
        }

        else -> {
            AppNavigation(
                content = content,
                participantId = state.participantId!!,
                onResetParticipant = viewModel::resetParticipant,
            )
        }
    }
}
