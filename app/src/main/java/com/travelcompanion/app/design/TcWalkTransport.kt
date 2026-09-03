package com.travelcompanion.app.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Walk Mode's transport (screen 07), on the ink bottom bar.
 *
 * Distinct from [TcAudioPlayer] on purpose: the approved prototype gives
 * screen 07 its own treatment — ink surface, white play button, 56 / 76 / 56dp
 * movement — while screen 09's shared player keeps the teal card at
 * 52 / 68 / 52dp. Sizes here are what a thumb finds without looking, because
 * this is used standing in the street.
 *
 * −15 / +15 is the surface D025 deferred seek to; the capability already lives
 * on `PlaybackController`.
 */
@Composable
fun TcWalkTransport(
    progress: Float,
    elapsed: String,
    total: String,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier,
    isBuffering: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(TcPillShape)
                .background(FieldCompanionColors.OnInkTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(FieldCompanionColors.TealBright),
            )
        }

        val timeStyle = TcType.label.copy(fontSize = 12.sp, fontFeatureSettings = "tnum")
        Row(
            modifier = Modifier
                .padding(top = 7.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(elapsed, style = timeStyle, color = FieldCompanionColors.OnInkMeta)
            Text(total, style = timeStyle, color = FieldCompanionColors.OnInkMeta)
        }

        Row(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkipButton("−15", "Voltar 15 segundos", onSkipBack)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(TcPillShape)
                    .background(FieldCompanionColors.White)
                    .clickable(onClick = onTogglePlayPause)
                    .semantics { contentDescription = if (isPlaying) "Pausar" else "Ouvir" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying || isBuffering) TcIcons.Pause else TcIcons.Play,
                    contentDescription = null,
                    tint = FieldCompanionColors.Ink,
                    modifier = Modifier.size(32.dp),
                )
            }
            SkipButton("+15", "Avançar 15 segundos", onSkipForward)
        }
    }
}

@Composable
private fun SkipButton(label: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TRANSPORT_SIDE_BUTTON)
            .clip(TcPillShape)
            .background(FieldCompanionColors.OnInkFill)
            .border(1.dp, FieldCompanionColors.OnInkBorder, TcPillShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TcType.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = FieldCompanionColors.White,
        )
    }
}

private val TRANSPORT_SIDE_BUTTON: Dp = 56.dp
