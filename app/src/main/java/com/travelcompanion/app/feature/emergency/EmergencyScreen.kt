package com.travelcompanion.app.feature.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.domain.operations.PhoneUi
import com.travelcompanion.app.feature.operations.DetailHeader

/**
 * Screen 17 — Emergência.
 *
 * High contrast, no bottom navigation, no editorial hierarchy: this is the one
 * screen in the app that is read by someone who is frightened, possibly by
 * someone else holding the phone. Everything on it is either a number to call
 * or a sentence to show a stranger.
 *
 * Nothing dials by itself. Every button opens the dialer with the number
 * filled in and stops there, which is `ACTION_DIAL` and needs no permission
 * (D064).
 */
@Composable
fun EmergencyScreen(
    state: EmergencyUiState,
    onBack: () -> Unit,
    onDial: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper)
            // No bottom navigation on this screen, so it takes the system bar
            // insets for itself — the shell reserves them once per route and
            // this is a route that draws to the bottom edge.
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        DetailHeader(title = "Emergência", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = state.location,
                style = TcType.body,
                color = FieldCompanionColors.Neutral700,
            )

            // 88dp, the largest touch target in the app, because it is the one
            // that gets pressed with shaking hands.
            PrimaryEmergencyButton(
                phone = state.general,
                worksWithoutCredit = state.generalWorksWithoutCredit,
                note = state.generalNote,
                onDial = onDial,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.police?.let { BigButton(it, Modifier.weight(1f), onDial) }
                state.ambulance?.let { BigButton(it, Modifier.weight(1f), onDial) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.contacts.forEach { ContactRow(it, onDial) }
            }

            state.showToSomeone?.let { ShowToSomeone(it) }
        }
    }
}

@Composable
private fun PrimaryEmergencyButton(
    phone: PhoneUi,
    worksWithoutCredit: Boolean,
    /**
     * What the country says about this number — that 112 is still being rolled
     * out in Bosnia, that Brazil has no single number. Secondary weight, inside
     * this block rather than beside it, because it is read by someone who has
     * just looked at the number it is about (D160).
     */
    note: String?,
    onDial: (String) -> Unit,
) {
    val number = phone.number
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Oxblood)
            .then(
                if (number != null) {
                    Modifier
                        .clickable { onDial(number) }
                        .semantics { contentDescription = phone.accessibilityLabel }
                } else {
                    Modifier
                },
            )
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.height(88.dp), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    TcIcons.Call,
                    contentDescription = null,
                    tint = FieldCompanionColors.White,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = phone.label,
                    style = TcType.headline.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                    color = FieldCompanionColors.White,
                )
            }
        }
        if (worksWithoutCredit) {
            Text(
                text = "Funciona sem crédito e sem chip local.",
                style = TcType.meta,
                color = FieldCompanionColors.White,
                textAlign = TextAlign.Center,
            )
        }
        note?.let {
            Text(
                text = it,
                style = TcType.meta,
                color = FieldCompanionColors.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
private fun BigButton(phone: PhoneUi, modifier: Modifier, onDial: (String) -> Unit) {
    val number = phone.number
    Column(
        modifier = modifier
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
            // 2dp of ink, which the approved sheet draws on these two and on
            // nothing else in the app. This is the screen that is read in a
            // hurry and possibly in sunlight, and the border is the contrast
            // that separates the police and ambulance numbers from the paper
            // behind them — it was missing entirely.
            .border(2.dp, FieldCompanionColors.Ink, TcCardShape)
            .then(
                if (number != null) {
                    Modifier
                        .clickable { onDial(number) }
                        .semantics { contentDescription = phone.accessibilityLabel }
                } else {
                    Modifier
                },
            )
            .padding(vertical = 18.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = phone.number ?: "—",
            style = TcType.clockLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
            color = FieldCompanionColors.Ink,
        )
        Text(
            text = phone.label,
            style = TcType.body,
            color = FieldCompanionColors.Neutral700,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * One of the three contacts, at the approved 48dp with its own accessibility
 * label — a screen reader on this screen says who is being called, not "botão".
 *
 * A contact line draws its packaged note when it has one, at the same weight
 * and in the same place D160 gave the general number's: under what it is about,
 * secondary, no new component and no colour outside the tokens. One rule, no
 * per-field list of which contacts are allowed a sentence (D175).
 */
@Composable
private fun ContactRow(phone: PhoneUi, onDial: (String) -> Unit) {
    val number = phone.number
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Surface)
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(TcPillShape)
                .background(
                    if (number != null) {
                        FieldCompanionColors.TealSoft
                    } else {
                        FieldCompanionColors.Neutral100
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                TcIcons.Call,
                contentDescription = null,
                tint = if (number != null) {
                    FieldCompanionColors.Teal
                } else {
                    FieldCompanionColors.Neutral400
                },
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = phone.label,
                style = TcType.body.copy(fontWeight = FontWeight.Bold),
                color = FieldCompanionColors.Ink,
            )
            phone.detail?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
            phone.note?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
            phone.contentNote?.let {
                Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
            }
        }
    }
}

/**
 * The ink card held up to a stranger.
 *
 * The local-language sentence is the large half, because the person reading it
 * does not speak the traveller's language; the translation underneath is for
 * the traveller, so they know what they are showing.
 */
@Composable
private fun ShowToSomeone(card: ShowToSomeoneUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TcCardShape)
            .background(FieldCompanionColors.Ink)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "MOSTRE ESTA TELA",
            style = TcType.eyebrow,
            color = FieldCompanionColors.OnInkAccent,
        )
        Text(
            text = card.localLanguageText,
            style = TcType.subheadline,
            color = FieldCompanionColors.White,
        )
        Text(
            text = card.translation,
            style = TcType.meta,
            color = FieldCompanionColors.OnInkMeta,
        )
    }
}
