package com.travelcompanion.app.feature.walk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcStateDot
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.design.TcWalkTransport

/**
 * Screen 07 — Passeio ativo.
 *
 * Read in two seconds, standing in the street, and mostly not read at all:
 * this is what the traveller sees when they take the phone out of a pocket,
 * not where they are expected to look. Hence the ink field, one large walking
 * instruction, and a transport sized for a thumb.
 */
@Composable
fun ActiveWalkScreen(
    state: ActiveWalkUiState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onListenTogether: () -> Unit,
    modifier: Modifier = Modifier,
    scaffold: WalkPrototypeScaffold? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Ink),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.progressLabel,
                style = TcType.bodyStrong.copy(fontFeatureSettings = "tnum"),
                color = FieldCompanionColors.White,
            )
            Text(
                text = state.remainingLabel,
                style = TcType.body,
                color = FieldCompanionColors.OnInkMeta,
                modifier = Modifier.padding(start = 10.dp),
            )
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TcPillShape)
                    .clickable(onClick = onClose)
                    .semantics { contentDescription = "Encerrar passeio" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TcIcons.Close,
                    contentDescription = null,
                    tint = FieldCompanionColors.OnInkMeta,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SegmentedProgress(total = state.totalStops, done = state.completedStops)

            Column {
                Row(
                    modifier = Modifier
                        .clip(TcPillShape)
                        .background(FieldCompanionColors.OnInkPill)
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TcStateDot(color = FieldCompanionColors.TealBright)
                    Text(
                        text = state.nowPlayingEyebrow.uppercase(),
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.OnInkAccent,
                    )
                }
                Text(
                    text = state.storyTitle,
                    style = TcType.heroEditorial,
                    color = FieldCompanionColors.White,
                    modifier = Modifier.padding(top = 14.dp),
                )
                state.storyContext?.let {
                    Text(
                        text = it,
                        style = TcType.body,
                        color = FieldCompanionColors.OnInkMeta,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            state.instruction?.let { instruction ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FieldCompanionColors.OnInkCard)
                        .padding(20.dp),
                ) {
                    Text("CONTINUE", style = TcType.eyebrow, color = FieldCompanionColors.OnInkEyebrow)
                    // The most legible thing on the screen, because it is the
                    // one line that gets read while walking.
                    Text(
                        text = instruction,
                        style = TcType.body.copy(fontSize = 22.sp, lineHeight = 30.sp),
                        color = FieldCompanionColors.White,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            state.nextStopLabel?.let { next ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, FieldCompanionColors.OnInkHairline, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = TcIcons.Pin,
                        contentDescription = null,
                        tint = FieldCompanionColors.OnInkAccent,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PRÓXIMA HISTÓRIA",
                            style = TcType.eyebrow,
                            color = FieldCompanionColors.OnInkEyebrow,
                        )
                        Text(
                            text = next,
                            style = TcType.action,
                            color = FieldCompanionColors.White,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TcStateDot(color = state.locationTone)
                Text(
                    text = state.locationLabel,
                    style = TcType.meta,
                    color = FieldCompanionColors.OnInkMeta,
                )
            }

            // Prototype scaffolding. The label and the action both come from
            // the debug variant, so a release build contributes neither
            // (D031).
            scaffold?.let { prototype ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, FieldCompanionColors.OnInkBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = prototype.onActivate)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = prototype.label,
                        style = TcType.meta,
                        color = FieldCompanionColors.OnInkMeta,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FieldCompanionColors.InkDeep)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TcWalkTransport(
                progress = state.audioProgress,
                elapsed = state.elapsed,
                total = state.total,
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onTogglePlayPause = onTogglePlayPause,
                onSkipBack = onSkipBack,
                onSkipForward = onSkipForward,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FieldCompanionColors.OnInkPill)
                    .clickable(onClick = onListenTogether),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = TcIcons.Group,
                    contentDescription = null,
                    tint = FieldCompanionColors.White,
                    modifier = Modifier.size(20.dp),
                )
                Text("Ouvir juntos", style = TcType.action, color = FieldCompanionColors.White)
            }
        }
    }
}

/** The walk's progress as one segment per stop, per the approved prototype. */
@Composable
private fun SegmentedProgress(total: Int, done: Int) {
    if (total <= 0) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(TcPillShape)
                    .background(
                        if (index < done) {
                            FieldCompanionColors.TealBright
                        } else {
                            FieldCompanionColors.OnInkTrack
                        },
                    ),
            )
        }
    }
}

/** Local alias so the screen does not import Color for one dot. */
private val ActiveWalkUiState.locationTone: Color
    get() = when (locationState) {
        WalkLocationUi.Active -> FieldCompanionColors.Moss
        WalkLocationUi.Searching -> FieldCompanionColors.Gold
        WalkLocationUi.Off -> FieldCompanionColors.Neutral500
    }
