package com.travelcompanion.app.feature.document

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelcompanion.app.design.FieldCompanionColors
import com.travelcompanion.app.design.TcCardShape
import com.travelcompanion.app.design.TcHeroShape
import com.travelcompanion.app.design.TcIcons
import com.travelcompanion.app.design.TcPillShape
import com.travelcompanion.app.design.TcPrimaryButton
import com.travelcompanion.app.design.TcSecondaryButton
import com.travelcompanion.app.design.TcType
import com.travelcompanion.app.design.rememberPackagedImage
import com.travelcompanion.app.service.documents.QrMatrix

/**
 * Screen 14 — Documento, ficha mode.
 *
 * A ticket drawn as a ticket, from the trip's own data rather than from the
 * file: times, passengers, platform, locator, price, and a dashed perforation
 * separating the body from the code. Drawing it from data is also what keeps
 * it readable when the file itself is not in this build (D060).
 */
@Composable
fun DocumentScreen(
    state: DocumentUiState,
    onBack: () -> Unit,
    onOpenQr: (() -> Unit)?,
    /**
     * Hands the packaged file to a viewer on the phone. Null when there is no
     * file in this build, which is the only reason the control is ever absent
     * (D091).
     */
    onOpenFile: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FieldCompanionColors.Paper),
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
            Text("Documento", style = TcType.action, color = FieldCompanionColors.Ink)
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.notPackagedNote?.let { note ->
                Text(
                    text = note,
                    style = TcType.meta,
                    color = FieldCompanionColors.GoldBody,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FieldCompanionColors.GoldSoft)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TcHeroShape)
                    .background(FieldCompanionColors.Surface)
                    .border(1.dp, FieldCompanionColors.Neutral200, TcHeroShape),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column {
                        Text(
                            text = state.title,
                            style = TcType.sectionTitle,
                            color = FieldCompanionColors.Ink,
                        )
                        state.subtitle?.let {
                            Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral600)
                        }
                    }

                    // One block per leg. A ticket titled "Voos São Paulo ⇄
                    // Amsterdã" promises two, and the Zagreb connection needs
                    // both on screen at once — its legs are 1h15 apart on the
                    // same morning (D155).
                    state.journey.forEach { Journey(it) }

                    // The two fields share the row rather than one taking
                    // what it likes; the label gives way before the value
                    // does, which is D055's rule about which half of a line
                    // survives. Without it "PLATAFORMA" broke across two lines
                    // at 360dp with the font at 1.5.
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        state.passengers?.let {
                            Field("Passageiros", it, modifier = Modifier.weight(1f))
                        }
                        // A platform belongs to a leg, so this field speaks
                        // only when the document leaves no doubt which leg it
                        // means. No packaged transport declares one.
                        state.journey.mapNotNull { it.platform }.distinct().singleOrNull()?.let {
                            Field("Plataforma", it, modifier = Modifier.weight(1f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        state.locator?.let {
                            Field("Localizador", it, tabular = true, modifier = Modifier.weight(1f))
                        }
                        state.price?.let { Field("Valor", it, modifier = Modifier.weight(1f)) }
                    }
                }

                // The perforation: a ticket tears here, and below it is what
                // gets held up at the counter.
                Perforation()

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    QrSummary(state.qr)
                    if (onOpenQr != null) {
                        TcPrimaryButton(onClick = onOpenQr, modifier = Modifier.fillMaxWidth()) {
                            Text("Abrir código")
                        }
                    }
                    // Secondary to the code: at a counter the code is what is
                    // scanned, and the file is what is asked for when it is
                    // not. Both, or whichever of the two this document has.
                    if (onOpenFile != null) {
                        TcSecondaryButton(onClick = onOpenFile, modifier = Modifier.fillMaxWidth()) {
                            Text("Abrir arquivo")
                        }
                    }
                    state.locatorNote?.let {
                        Text(
                            text = it,
                            style = TcType.meta,
                            color = FieldCompanionColors.Neutral600,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One leg: two ends, and who runs it.
 *
 * The operator line is grouped with its own leg rather than left as a sibling
 * of the card's 14dp column — with two legs on screen, an operator floating
 * halfway between them belongs to neither.
 */
@Composable
private fun Journey(journey: JourneyUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = journey.originTime,
                    style = TcType.clockLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    color = FieldCompanionColors.Ink,
                )
                Text(
                    text = journey.originName,
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
            Icon(
                imageVector = TcIcons.AltRoute,
                contentDescription = null,
                tint = FieldCompanionColors.Neutral400,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = journey.destinationTime,
                    style = TcType.clockLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    color = FieldCompanionColors.Ink,
                )
                Text(
                    text = journey.destinationName,
                    style = TcType.meta,
                    color = FieldCompanionColors.Neutral600,
                )
            }
        }
        journey.operator?.let {
            Text(it, style = TcType.meta, color = FieldCompanionColors.Neutral500)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    tabular: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = TcType.eyebrow,
            color = FieldCompanionColors.Neutral600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = if (tabular) {
                TcType.body.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
            } else {
                TcType.body
            },
            color = FieldCompanionColors.Ink,
        )
    }
}

/** The dashed tear line the approved ticket is cut along. */
@Composable
private fun Perforation() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
    ) {
        drawLine(
            color = Color(0xFFDDDCD4),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
    }
}

/** What the ficha says about the code, before anyone opens it. */
@Composable
private fun QrSummary(qr: QrState) {
    val message = when (qr) {
        is QrState.Ready -> null
        QrState.None -> "Este documento não tem código."
        // §18, said to the traveller rather than hidden: the package is
        // carrying example data, so there is nothing real to encode (D061).
        QrState.MockContent ->
            "O código chega junto com a passagem real. Estes dados são de exemplo."
        QrState.AssetMissing -> "O código deste documento não está neste aparelho."
    }
    message?.let {
        Text(
            text = it,
            style = TcType.meta,
            color = FieldCompanionColors.Neutral600,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Screen 14 — QR mode.
 *
 * White, full screen, minimal: brief §18's list, and the reason for each item
 * is the same — a code is read by a camera held by someone else, in a queue.
 * The brightness and keep-awake behaviour is the route's, so that closing by
 * the system back button restores it just as the X does.
 */
@Composable
fun QrScreen(
    matrix: QrMatrix?,
    embeddedAssetPath: String?,
    locator: String?,
    summary: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        // A packaged code is shown as it was shipped; a generated one is
        // painted from its modules. Both at the approved 300dp.
        when {
            embeddedAssetPath != null -> rememberPackagedImage(embeddedAssetPath)?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier.size(300.dp),
                )
            }
            matrix != null -> QrCanvas(matrix, modifier = Modifier.size(300.dp))
        }
        locator?.let {
            Text(
                text = it,
                style = TcType.body.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = Color.Black,
            )
        }
        Text(
            text = summary,
            style = TcType.meta,
            color = FieldCompanionColors.Neutral700,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Brilho no máximo · tela não apaga",
            style = TcType.meta,
            color = FieldCompanionColors.Neutral500,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .clip(TcCardShape)
                .clickable(onClick = onClose)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .semantics { contentDescription = "Fechar código" },
        ) {
            Text("Fechar", style = TcType.action, color = FieldCompanionColors.Ink)
        }
    }
}

/** Paints the module grid. Black on white, and nothing else on the screen. */
@Composable
private fun QrCanvas(matrix: QrMatrix, modifier: Modifier = Modifier) {
    val modules = remember(matrix) { matrix.size }
    Canvas(modifier = modifier) {
        val module = size.minDimension / modules
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (!matrix.isDark(x, y)) continue
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(x * module, y * module),
                    size = Size(module, module),
                )
            }
        }
    }
}
