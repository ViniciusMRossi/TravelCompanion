package com.travelcompanion.app.feature.memory

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcHeroShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.memory.MemoryPhase

/**
 * Screen 12 — Gravar memória por voz.
 *
 * Voice is the primary format and there is no text field: the approved sheet
 * says so outright, and a memory is never asked for a title. The three states
 * share one place on the screen, which is what the sheet draws — the button
 * becomes the timer becomes the confirmation, and the list below never moves.
 */
@Composable
fun MemoryScreen(
    state: MemoryUiState,
    level: Float,
    onBack: () -> Unit,
    onRecord: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(TcPillShape)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TcIcons.ArrowLeft,
                    contentDescription = null,
                    tint = FieldCompanionColors.Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text("Memória", style = TcType.action, color = FieldCompanionColors.Ink)
        }

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // City, place and hour — the context the sheet puts at the top, and
            // the same three things the memory will carry by itself.
            Text(
                text = state.contextLine,
                style = TcType.meta,
                color = FieldCompanionColors.Neutral600,
            )

            RecorderCard(
                state = state,
                level = level,
                onRecord = onRecord,
                onPause = onPause,
                onResume = onResume,
                onFinish = onFinish,
                onCancel = onCancel,
                onDone = onDone,
            )

            if (state.previous.isNotEmpty()) {
                Text(
                    text = "Memórias desta viagem",
                    style = TcType.sectionTitle,
                    color = FieldCompanionColors.Ink,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.previous.forEach { SavedMemoryRow(it) }
                }
            }
        }
    }
}

@Composable
private fun RecorderCard(
    state: MemoryUiState,
    level: Float,
    onRecord: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcHeroShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcHeroShape)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state.phase) {
            MemoryPhase.Idle, MemoryPhase.Failed -> IdleRecorder(state, onRecord)
            MemoryPhase.Recording, MemoryPhase.Paused -> ActiveRecorder(
                state = state,
                level = level,
                onPause = onPause,
                onResume = onResume,
                onFinish = onFinish,
                onCancel = onCancel,
            )
            MemoryPhase.Saved -> SavedRecorder(state, onDone)
        }
    }
}

@Composable
private fun IdleRecorder(state: MemoryUiState, onRecord: () -> Unit) {
    // 82dp in oxblood with a 28dp white core, as the gallery specifies.
    Box(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 18.dp)
            .size(82.dp)
            .clip(TcPillShape)
            .background(FieldCompanionColors.Oxblood)
            .clickable(onClick = onRecord)
            .semantics { contentDescription = "Registrar memória" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(TcPillShape)
                .background(FieldCompanionColors.White),
        )
    }
    Text("Registrar memória", style = TcType.sectionTitle, color = FieldCompanionColors.Ink)
    Text(
        text = "O local e a hora entram sozinhos.",
        style = TcType.body,
        color = FieldCompanionColors.Neutral600,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp),
    )
    state.note?.let { note ->
        Text(
            text = note,
            style = TcType.meta,
            color = FieldCompanionColors.GoldBody,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldCompanionColors.GoldSoft)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ActiveRecorder(
    state: MemoryUiState,
    level: Float,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
) {
    val recording = state.phase == MemoryPhase.Recording
    TcChip(
        text = if (recording) "Gravando" else "Pausado",
        tone = if (recording) TcChipTone.Critical else TcChipTone.Neutral,
    )
    Text(
        text = state.elapsedLabel,
        style = TcType.clockLarge.copy(fontSize = 38.sp, fontWeight = FontWeight.Bold),
        color = FieldCompanionColors.Ink,
        modifier = Modifier.padding(top = 16.dp),
    )
    Waveform(level = level, animated = recording)
    Text(
        text = state.attributionLine,
        style = TcType.meta,
        color = FieldCompanionColors.Neutral600,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 18.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TcSecondaryButton(onClick = if (recording) onPause else onResume) {
            Text(if (recording) "Pausar" else "Retomar")
        }
        TcPrimaryButton(onClick = onFinish) { Text("Concluir") }
    }
    Text(
        text = "Cancelar",
        style = TcType.action,
        color = FieldCompanionColors.Oxblood,
        modifier = Modifier
            .padding(top = 14.dp)
            .clip(TcPillShape)
            .clickable(onClick = onCancel)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * Eight bars, as the gallery draws them.
 *
 * Their resting heights are the gallery's; the microphone's level scales them
 * together, so what moves on screen is what the microphone is hearing rather
 * than a decoration that would look identical with the mic muted.
 */
@Composable
private fun Waveform(level: Float, animated: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val sway by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700, easing = LinearEasing)),
        label = "sway",
    )
    val scale = if (animated) (0.35f + level * 0.9f) * sway else 0.35f

    Row(
        modifier = Modifier
            .height(44.dp)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RESTING_BARS.forEach { resting ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((resting * scale).dp.coerceAtLeast(3.dp))
                    .clip(TcPillShape)
                    .background(FieldCompanionColors.Oxblood.copy(alpha = 0.78f)),
            )
        }
    }
}

private val RESTING_BARS = listOf(10, 26, 38, 20, 34, 15, 29, 11)

@Composable
private fun SavedRecorder(state: MemoryUiState, onDone: () -> Unit) {
    TcChip(text = "Memória salva", tone = TcChipTone.Moss)
    val saved = state.savedConfirmation
    if (saved != null) {
        Text(
            text = saved.place,
            style = TcType.sectionTitle,
            color = FieldCompanionColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "${saved.time} · ${saved.duration}",
            style = TcType.body,
            color = FieldCompanionColors.Neutral600,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )
    }
    state.note?.let { note ->
        Text(
            text = note,
            style = TcType.meta,
            color = FieldCompanionColors.GoldBody,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldCompanionColors.GoldSoft)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
    TcPrimaryButton(onClick = onDone) { Text("Voltar para Hoje") }
}

@Composable
private fun SavedMemoryRow(memory: SavedMemoryUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcCardShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = memory.title,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
                maxLines = 1,
            )
            Text(
                text = "${memory.date} · ${memory.author}",
                style = TcType.meta,
                color = FieldCompanionColors.Neutral600,
                maxLines = 1,
            )
        }
        Text(
            text = memory.duration,
            style = TcType.meta,
            color = FieldCompanionColors.Neutral600,
        )
    }
}
