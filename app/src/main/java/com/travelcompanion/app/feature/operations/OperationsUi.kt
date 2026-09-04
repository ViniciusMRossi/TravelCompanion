package com.travelcompanion.app.feature.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelcompanion.app.data.trip.CriticalItem
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.operations.ActionWindow
import com.travelcompanion.app.domain.operations.PhoneUi

/**
 * The pieces screens 15 and 16 share.
 *
 * Screen 02 has a critical card of its own and it is deliberately left alone:
 * it is verified on hardware and refactoring it to share this one would put a
 * proven screen at risk for tidiness. These are drawn to the same approved
 * treatment — oxblood rule, the soft critical fill, timing kept apart from
 * anything editorial.
 */

/** The back row every detail screen in this block opens with. */
@Composable
fun DetailHeader(title: String, onBack: () -> Unit) {
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
        Text(title, style = TcType.action, color = FieldCompanionColors.Ink)
    }
}

/**
 * A critical item, with its timing apart from its instruction.
 *
 * The nominal time and the act-by instruction are two different facts and the
 * approved screens never let one stand in for the other. [window] says which
 * of the two the traveller is inside, which is the only thing here that is a
 * decision rather than a layout.
 */
@Composable
fun CriticalBlock(
    item: CriticalItem,
    window: ActionWindow,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The rule down the left is a child that fills the row's height,
            // and a row inside a scrolling column has no height to fill until
            // its content decides one — `fillMaxHeight` resolved to nothing and
            // the rule did not draw at all. `IntrinsicSize.Min` is what the
            // timeline on screen 02 already uses for the same shape; this is
            // D051's defect in a second place (D066).
            .height(IntrinsicSize.Min)
            .clip(TcCardShape)
            .background(FieldCompanionColors.CriticalSoft)
            .border(1.dp, FieldCompanionColors.CriticalBorder, TcCardShape),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(FieldCompanionColors.Oxblood),
        )
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    TcIcons.Warning,
                    contentDescription = null,
                    tint = FieldCompanionColors.Oxblood,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "NÃO PODE DAR ERRADO",
                    style = TcType.eyebrow,
                    color = FieldCompanionColors.Oxblood,
                )
                if (window == ActionWindow.Now) {
                    Text(
                        text = "AGORA",
                        style = TcType.eyebrow,
                        color = FieldCompanionColors.GoldInk,
                        modifier = Modifier
                            .clip(TcPillShape)
                            .background(FieldCompanionColors.GoldSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            // The scheduled time, on its own line and never merged with the
            // instruction below it.
            item.nominalTime?.let {
                Text(
                    text = it,
                    style = TcType.clockLarge,
                    color = FieldCompanionColors.Ink,
                )
            }
            Text(
                text = item.title,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
            )
            item.instruction?.let {
                Text(it, style = TcType.body, color = FieldCompanionColors.CriticalBody)
            }
            item.reason?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.CriticalBody)
            }
            actions()
        }
    }
}

/** A 48dp row that opens something. */
@Composable
fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcCardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = FieldCompanionColors.Teal,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = TcType.body,
            color = FieldCompanionColors.Ink,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            TcIcons.ArrowRight,
            contentDescription = null,
            tint = FieldCompanionColors.Neutral400,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * A telephone row, which dials only when there is a number to dial.
 *
 * A withheld number keeps its row — knowing the operator or the insurer has a
 * line matters even when this build cannot dial it — and says why, instead of
 * sitting there as a button that does nothing (D064).
 */
@Composable
fun PhoneRow(phone: PhoneUi, onDial: (String) -> Unit) {
    val number = phone.number
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, TcCardShape)
            .then(
                if (number != null) {
                    Modifier
                        .clickable { onDial(number) }
                        .semantics { contentDescription = phone.accessibilityLabel }
                } else {
                    Modifier
                },
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            TcIcons.Call,
            contentDescription = null,
            tint = if (number != null) FieldCompanionColors.Teal else FieldCompanionColors.Neutral400,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(phone.label, style = TcType.body, color = FieldCompanionColors.Ink)
            phone.detail?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
            phone.note?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
        }
    }
}

/** One label-and-value of a metadata footer. */
@Composable
fun Meta(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = TcType.eyebrow,
            color = FieldCompanionColors.Neutral600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = value, style = TcType.body, color = FieldCompanionColors.Ink)
    }
}

/** The soft card the closing sections of 15 and 16 sit in. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FieldCompanionColors.Surface)
            .border(1.dp, FieldCompanionColors.Neutral200, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}
