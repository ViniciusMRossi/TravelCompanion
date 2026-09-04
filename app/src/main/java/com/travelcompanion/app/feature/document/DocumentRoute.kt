package com.travelcompanion.app.feature.document

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.travelcompanion.app.data.trip.TripContent
import com.travelcompanion.app.feature.placeholder.PlaceholderScreen
import com.travelcompanion.app.service.documents.encodeQr

/**
 * Screen 14 as a route: the ficha, and the code held up over it.
 *
 * QR mode is a state of this screen rather than a destination of its own —
 * the approved flow is one action in and "fechar" back out, and making it a
 * back-stack entry would mean two ways to leave with two chances to forget
 * the brightness.
 */
@Composable
fun DocumentRoute(
    content: TripContent,
    documentId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = buildDocumentState(content, documentId)
    if (state == null) {
        PlaceholderScreen(
            title = "Documento",
            message = "Este documento não existe no conteúdo desta viagem.",
            actionLabel = "Voltar",
            onAction = onBack,
        )
        return
    }

    var showingQr by rememberSaveable(documentId) { mutableStateOf(false) }
    val ready = state.qr as? QrState.Ready
    val generated = ready as? QrState.Generated

    // Encoded once per code, not once per frame: a 512-module matrix is real
    // work and the text does not change while the screen is open.
    val matrix = remember(generated?.text) { generated?.text?.let(::encodeQr) }

    if (showingQr && ready != null) {
        BrightScreen()
        // Leaving by the system back button has to restore exactly what the
        // "Fechar" button restores. §18 names the restoration and it is the
        // half that gets forgotten (D062).
        BackHandler { showingQr = false }
        QrScreen(
            matrix = matrix,
            embeddedAssetPath = (ready as? QrState.Embedded)?.assetPath,
            locator = state.locator,
            summary = listOfNotNull(state.title, state.subtitle).joinToString(" · "),
            onClose = { showingQr = false },
            modifier = modifier,
        )
        return
    }

    DocumentScreen(
        state = state,
        onBack = onBack,
        // Offered only when there is a code to show. A control that opens an
        // empty white screen is worse than no control.
        onOpenQr = if (ready != null) ({ showingQr = true }) else null,
        modifier = modifier,
    )
}

/**
 * Full brightness and no screen timeout, for as long as the code is up.
 *
 * Both are window attributes, and both are undone by the same
 * `DisposableEffect` — leaving by any route disposes it, which is how the
 * back button and the close button end up doing the same thing (brief §18).
 */
@Composable
internal fun BrightScreen() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val previous = window?.attributes?.screenBrightness
        window?.let {
            it.attributes = it.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.let {
                it.attributes = it.attributes.apply {
                    // Back to whatever it was, which is normally
                    // BRIGHTNESS_OVERRIDE_NONE — the system's own setting.
                    screenBrightness = previous
                        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
