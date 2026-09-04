package com.travelcompanion.app.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcChip
import com.travelcompanion.app.design.TcChipTone
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.today.BookingStatusUi
import com.travelcompanion.app.domain.wallet.WalletRow
import com.travelcompanion.app.domain.wallet.WalletUiState

/**
 * Screen 13 — Carteira.
 *
 * Every document of the trip, grouped by how soon it matters rather than by
 * what kind of file it is. The header counts them and says whether the whole
 * wallet is on the phone; the pill is absent, not softened, when it is not
 * (D013, D060).
 */
@Composable
fun WalletScreen(
    state: WalletUiState,
    onOpenDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Carteira", style = TcType.titleEditorial, color = FieldCompanionColors.Ink)
                Text(
                    text = documentCount(state.totalCount),
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
            // Only when every document really resolves. A header that claims
            // the whole wallet is on the phone while one file is missing is
            // the lie D013 refused to tell on screen 05.
            if (state.allOffline) {
                TcChip(text = "Tudo offline", tone = TcChipTone.Moss, icon = TcIcons.Offline)
            }
        }

        state.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = section.group.title.uppercase(),
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Neutral600,
                )
                section.rows.forEach { row ->
                    DocumentRow(row) { onOpenDocument(row.documentId) }
                }
            }
        }
    }
}

private fun documentCount(total: Int): String =
    if (total == 1) "1 documento" else "$total documentos"

@Composable
private fun DocumentRow(row: WalletRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The approved row is a 48dp touch target, which is also the
            // token file's minimum.
            .heightIn(min = 48.dp)
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcCardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(TcCardShape)
                .background(FieldCompanionColors.Neutral100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(row.type),
                contentDescription = null,
                tint = FieldCompanionColors.Neutral700,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(
                row.detail,
                // Said on the row it is true about, not hidden behind the
                // missing header pill.
                if (!row.resolvesOffline) "arquivo não está neste aparelho" else null,
            ).joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        row.status?.let { status ->
            TcChip(text = status.label, tone = status.tone())
        }
    }
}

/** The S5 booking tones, unchanged: status is never carried by colour alone. */
private fun BookingStatusUi.tone(): TcChipTone = when (this) {
    BookingStatusUi.Reserved, BookingStatusUi.Paid, BookingStatusUi.Included -> TcChipTone.Moss
    BookingStatusUi.Verify -> TcChipTone.Gold
    BookingStatusUi.Buy -> TcChipTone.Teal
}

/**
 * The icon set has no bed and no shield, and this is not the commit that
 * decides what those look like. A document with no icon of its own gets the
 * wallet's, which is at least true.
 */
private fun iconFor(type: String): ImageVector = when (type) {
    "ticket" -> TcIcons.Ticket
    else -> TcIcons.Wallet
}
