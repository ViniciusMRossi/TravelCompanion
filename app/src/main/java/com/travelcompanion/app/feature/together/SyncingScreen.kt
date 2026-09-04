package com.travelcompanion.app.feature.together

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcStateDot
import com.travelcompanion.app.design.TcType

/** One person on screen 08, with the state the design gives them. */
data class SyncingParticipantUi(
    val initial: String,
    val name: String,
    val label: String,
)

/**
 * Screen 08 — Participantes sincronizados.
 *
 * A three-second transition, not a screen anyone reads. It answers one
 * question — are we both about to hear the same thing — and then gets out of
 * the way.
 *
 * What it deliberately never shows: network name, signal strength, protocol,
 * host or client, latency. Synchronization describes itself by its result.
 */
@Composable
fun SyncingScreen(
    storyTitle: String,
    participants: List<SyncingParticipantUi>,
    countdown: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Ink)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterVertically),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "OUVIR JUNTOS",
                style = TcType.eyebrowWide,
                color = FieldCompanionColors.OnInkAccent,
            )
            Text(
                text = storyTitle,
                style = TcType.titleEditorial,
                color = FieldCompanionColors.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            participants.forEach { participant ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(TcPillShape)
                            .background(FieldCompanionColors.OnInkPill)
                            .border(2.dp, FieldCompanionColors.Moss, TcPillShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = participant.initial,
                            style = TcType.headline.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            color = FieldCompanionColors.White,
                        )
                    }
                    Text(
                        text = participant.name,
                        style = TcType.metaStrong.copy(fontSize = 14.sp),
                        color = FieldCompanionColors.White,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        TcStateDot(color = FieldCompanionColors.Moss)
                        Text(
                            text = participant.label,
                            style = TcType.label,
                            color = FieldCompanionColors.OnInkMeta,
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(TcPillShape)
                    .border(2.dp, FieldCompanionColors.OnInkBorder, TcPillShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = countdown?.toString().orEmpty(),
                    style = TcType.clockLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = FieldCompanionColors.White,
                )
            }
            Text(
                text = "Começando juntos",
                style = TcType.action,
                color = FieldCompanionColors.OnInkBody,
            )
        }

        Text(
            text = "Os dois aparelhos vão tocar a mesma parte da história. Podem guardar o celular.",
            style = TcType.meta,
            color = FieldCompanionColors.OnInkMeta,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}
