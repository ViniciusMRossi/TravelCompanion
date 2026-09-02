package com.travelcompanion.app.feature.whoareyou

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.data.trip.Participant
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcAvatar
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcHero
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcType

/**
 * Screen 01 — Quem é você?
 *
 * First run only. This identifies which participant holds this phone; it is
 * not a login. Both people already belong to the trip group, so one tap
 * commits the choice and goes straight to Today — no confirmation step.
 */
@Composable
fun WhoAreYouScreen(
    content: TripContent,
    onParticipantSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        TcHero(
            modifier = Modifier
                .fillMaxWidth()
                .height(262.dp),
            imageAssetPath = content.assets.packagedPathIfPresent(content.info.coverAssetId),
            placeholderCaption = coverCaption(content),
        )

        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content.info.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle.uppercase(),
                        style = TcType.eyebrowWide,
                        color = FieldCompanionColors.Gold,
                    )
                }
                Text(
                    text = "Quem é você?",
                    style = TcType.displayEditorial,
                    color = FieldCompanionColors.Ink,
                )
                Text(
                    text = "Roteiro, audioguias e documentos já estão neste aparelho. " +
                        "Escolha quem está com este celular.",
                    style = TcType.body,
                    color = FieldCompanionColors.Neutral700,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content.info.participants.forEach { participant ->
                    ParticipantRow(
                        participant = participant,
                        onClick = { onParticipantSelected(participant.id) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = TcIcons.Offline,
                    contentDescription = null,
                    tint = FieldCompanionColors.Neutral600,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(18.dp),
                )
                Text(
                    text = "Offline · a viagem inteira já está salva neste aparelho. " +
                        "Você pode trocar de pessoa depois em Configurações.",
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    participant: Participant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral300, TcCardShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Sou ${participant.name}" }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TcAvatar(initial = participant.initial)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = participant.name,
                style = TcType.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
            )
            participant.subtitle?.let {
                Text(text = it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
        }
        Icon(
            imageVector = TcIcons.ChevronRight,
            contentDescription = null,
            tint = FieldCompanionColors.Neutral500,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Names the photograph still to be produced for this hero. */
private fun coverCaption(content: TripContent): String {
    val description = content.assets.asset(content.info.coverAssetId)?.description
    return "foto — ${description ?: content.info.title}"
}
