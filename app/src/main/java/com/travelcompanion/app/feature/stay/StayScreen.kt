package com.travelcompanion.app.feature.stay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.data.trip.ActionLink
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcHero
import com.travelcompanion.app.design.TcHeroShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.feature.operations.ActionRow
import com.travelcompanion.app.feature.operations.CriticalBlock
import com.travelcompanion.app.feature.operations.DetailHeader
import com.travelcompanion.app.feature.operations.Meta
import com.travelcompanion.app.feature.operations.PhoneRow
import com.travelcompanion.app.feature.operations.SoftCard

/**
 * Screen 16 — Hospedagem.
 *
 * The hero draws the approved striped placeholder while no photography is
 * packaged, and the name uses the serif fallback while Fraunces is absent.
 * Both are Phase 1 items still open (D005) rather than divergences, so the
 * screen is drawn as the sheet asks and the two substitutes show.
 */
@Composable
fun StayScreen(
    state: StayUiState,
    onBack: () -> Unit,
    onAction: (ActionLink) -> Unit,
    onDial: (String) -> Unit,
    onOpenVoucher: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
    ) {
        DetailHeader(title = "Hospedagem", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TcHero(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(TcHeroShape),
                // The lodging's name and its booking chip sit on the photograph.
                titleOverPhotograph = true,
                imageAssetPath = state.heroAssetPath,
                placeholderCaption = state.heroCaption,
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.name,
                        style = TcType.subheadline,
                        color = FieldCompanionColors.White,
                    )
                    state.status?.let { TcChip(text = it.label, tone = TcChipTone.Moss) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SoftCard(modifier = Modifier.weight(1f)) {
                    Meta("Check-in", state.checkIn)
                }
                SoftCard(modifier = Modifier.weight(1f)) {
                    Meta("Check-out", state.checkOut)
                }
            }

            state.critical?.let { critical ->
                CriticalBlock(item = critical, window = state.window) {
                    state.hostPhone?.let { PhoneRow(it, onDial) }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.voucherId?.let { id ->
                    ActionRow(TcIcons.Document, "Voucher offline") { onOpenVoucher(id) }
                }
                state.actions.forEach { action ->
                    ActionRow(TcIcons.Map, action.label) { onAction(action) }
                }
            }

            state.instructions?.let { instructions ->
                SoftCard {
                    Text(
                        text = "INSTRUÇÕES DO ANFITRIÃO",
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.Neutral600,
                    )
                    Text(
                        text = instructions,
                        style = TcType.editorialBody,
                        color = FieldCompanionColors.Neutral800,
                    )
                }
            }
        }
    }
}
