package com.travelcompanion.app.feature.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType

/**
 * Screen 10 — História disparada pela localização.
 *
 * An editorial interruption during a walk. The walk stays visible at the top,
 * dimmed, and the story rises from the bottom on a light ground: the same
 * separation between operation and narrative the rest of the app keeps, drawn
 * here as composition.
 *
 * It exists to be read by someone who has just taken the phone out of a
 * pocket, so nothing on it depends on having been watched, and *Depois* costs
 * nothing — a location story is enrichment and never navigation.
 */
@Composable
fun StoryTriggerSheet(
    state: StoryTriggerUiState,
    onListen: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reading by remember(state.title) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Ink.copy(alpha = 0.55f))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // The walk carries on above and is *screen 07 itself*, dimmed by this
        // scrim. Drawing a second progress line here put "1 de 2 · ≈ 22 min
        // restantes" on top of the identical line the screen underneath was
        // already showing, and on the emulator the two overprinted into
        // nonsense (D078).
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(FieldCompanionColors.Paper)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .clip(TcPillShape)
                    .background(FieldCompanionColors.TealSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    TcIcons.Pin,
                    contentDescription = null,
                    tint = FieldCompanionColors.Teal,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "História pelo caminho",
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Teal,
                )
            }

            Text(
                text = state.title,
                style = TcType.titleEditorial.copy(fontSize = 32.sp),
                color = FieldCompanionColors.Ink,
            )
            // *Ler* opens the whole story where it stands: the traveller is
            // mid-walk with the phone in one hand, and sending them to another
            // screen to read is the opposite of what this sheet is for.
            Text(
                text = if (reading) state.fullText else state.body,
                style = TcType.editorialBody,
                color = FieldCompanionColors.Neutral800,
            )

            // The operational facts, on their own line and never inside the
            // paragraph above.
            if (state.operationalLine.isNotBlank()) {
                Text(
                    text = state.operationalLine,
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }

            // Three labels never fit on one line at 360dp with the font at
            // 1.5, and a squeezed button is the defect D066 and D072 both
            // recorded. Each keeps its whole label and takes the next line.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.hasAudio) {
                    TcPrimaryButton(onClick = onListen) { Text("Ouvir agora") }
                }
                TcSecondaryButton(onClick = { reading = !reading }) {
                    Text(if (reading) "Fechar" else "Ler")
                }
                TcSecondaryButton(onClick = onLater) { Text("Depois") }
            }

            Text(
                text = state.footnote,
                style = TcType.meta,
                color = FieldCompanionColors.Neutral600,
                textAlign = TextAlign.Start,
            )
        }
    }
}
