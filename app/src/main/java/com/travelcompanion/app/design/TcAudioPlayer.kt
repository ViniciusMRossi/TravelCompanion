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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The approved audioguide player (Field Companion `TcAudioPlayer`).
 *
 * Teal family, 18dp radius and padding, 6dp progress track. Audio is a
 * first-class surface here, never a generic media widget.
 *
 * Two approved variants, per the design system:
 * - [TcAudioPlayerVariant.Compact] — the persistent player that stays
 *   available while navigating (46dp toggle, no transport).
 * - [TcAudioPlayerVariant.Full] — the shared audioguide screen's player
 *   (52 / 68 / 52dp movement transport), screen 09. Screen 07 does not use it:
 *   the approved prototype gives Walk Mode its own ink transport at
 *   56 / 76 / 56dp, which is [TcWalkTransport].
 */
enum class TcAudioPlayerVariant { Compact, Full }

@Composable
fun TcAudioPlayer(
    title: String,
    progress: Float,
    elapsed: String,
    total: String,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isBuffering: Boolean = false,
    variant: TcAudioPlayerVariant = TcAudioPlayerVariant.Compact,
    onSkipBack: (() -> Unit)? = null,
    onSkipForward: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(FieldCompanionColors.TealSoft)
            .border(1.dp, FieldCompanionColors.TealBorder, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        if (variant == TcAudioPlayerVariant.Compact) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayToggle(
                    size = 46.dp,
                    iconSize = 19.dp,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onClick = onTogglePlayPause,
                )
                Column(modifier = Modifier.weight(1f)) {
                    PlayerTitle(title)
                    PlayerSubtitle(subtitle, topPadding = 2.dp)
                }
            }
        } else {
            PlayerTitle(title)
            PlayerSubtitle(subtitle, topPadding = 3.dp)
        }

        Box(
            modifier = Modifier
                .padding(
                    top = if (variant == TcAudioPlayerVariant.Compact) 15.dp else 16.dp,
                    bottom = 8.dp,
                )
                .fillMaxWidth()
                .height(6.dp)
                .clip(TcPillShape)
                .background(FieldCompanionColors.TealTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(FieldCompanionColors.Teal),
            )
        }

        val timeStyle = TcType.label.copy(
            fontSize = if (variant == TcAudioPlayerVariant.Compact) 11.sp else 12.sp,
            fontFeatureSettings = "tnum",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(elapsed, style = timeStyle, color = FieldCompanionColors.Neutral600)
            Text(total, style = timeStyle, color = FieldCompanionColors.Neutral600)
        }

        if (variant == TcAudioPlayerVariant.Full) {
            Row(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkipButton("−15", "Voltar 15 segundos", onSkipBack)
                PlayToggle(
                    size = 68.dp,
                    iconSize = 28.dp,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    onClick = onTogglePlayPause,
                )
                SkipButton("+15", "Avançar 15 segundos", onSkipForward)
            }
        }
    }
}

@Composable
private fun PlayerTitle(title: String) {
    Text(
        text = title,
        style = TcType.bodyStrong,
        color = FieldCompanionColors.Ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PlayerSubtitle(subtitle: String?, topPadding: androidx.compose.ui.unit.Dp) {
    subtitle ?: return
    Text(
        text = subtitle,
        style = TcType.label,
        color = FieldCompanionColors.Neutral600,
        modifier = Modifier.padding(top = topPadding),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PlayToggle(
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(TcPillShape)
            .background(FieldCompanionColors.Teal)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isPlaying) "Pausar" else "Ouvir" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying || isBuffering) TcIcons.Pause else TcIcons.Play,
            contentDescription = null,
            tint = FieldCompanionColors.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SkipButton(
    label: String,
    description: String,
    onClick: (() -> Unit)?,
) {
    onClick ?: return
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(TcPillShape)
            .background(FieldCompanionColors.Surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TcType.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = FieldCompanionColors.TealDark,
        )
    }
}

/** mm:ss for a position or duration. Unknown durations read as `--:--`. */
fun formatPlaybackTime(millis: Long): String {
    if (millis < 0L) return "--:--"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
