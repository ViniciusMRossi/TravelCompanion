package com.travelcompanion.app.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Field Companion icon set, converted 1:1 from the approved sprite
 * (`assets/icons/tc-icons.svg` in the prototype package).
 *
 * All icons are 24×24, 2dp stroke, round caps and joins. They are drawn in
 * black and tinted by [androidx.compose.material3.Icon], so a caller sets
 * colour through `tint`, never by editing an icon.
 */
object TcIcons {

    val ArrowLeft: ImageVector by lazy {
        icon("tc-arrow-left", stroke = "M20 12 L5 12 M11 6 L5 12 L11 18")
    }

    val ChevronRight: ImageVector by lazy {
        icon("tc-chevron-right", stroke = "M9 5 L16 12 L9 19")
    }

    val Pin: ImageVector by lazy {
        icon(
            "tc-pin",
            stroke = "M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 0 1 14 0Z " +
                "M9.5 10 a2.5 2.5 0 1 0 5 0 a2.5 2.5 0 1 0 -5 0 z",
        )
    }

    /** Used for the neutral "Offline · conteúdo disponível" state. */
    val Offline: ImageVector by lazy {
        icon(
            "tc-offline",
            stroke = "M4 12 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 z M6.4 6.4 L17.6 17.6",
        )
    }

    val Sun: ImageVector by lazy {
        icon(
            "tc-sun",
            stroke = "M8 12 a4 4 0 1 0 8 0 a4 4 0 1 0 -8 0 z " +
                "M12 3 L12 5.4 M12 18.6 L12 21 M3 12 L5.4 12 M18.6 12 L21 12 " +
                "M5.6 5.6 L7.3 7.3 M16.7 16.7 L18.4 18.4 " +
                "M18.4 5.6 L16.7 7.3 M7.3 16.7 L5.6 18.4",
        )
    }

    val Headphones: ImageVector by lazy {
        icon(
            "tc-headphones",
            stroke = "M4 15v-3a8 8 0 0 1 16 0v3 " +
                "M4.8 14 h0 a2.3 2.3 0 0 1 2.3 2.3 v2.4 a2.3 2.3 0 0 1 -4.6 0 v-2.4 a2.3 2.3 0 0 1 2.3 -2.3 z " +
                "M19.2 14 h0 a2.3 2.3 0 0 1 2.3 2.3 v2.4 a2.3 2.3 0 0 1 -4.6 0 v-2.4 a2.3 2.3 0 0 1 2.3 -2.3 z",
        )
    }

    val Warning: ImageVector by lazy {
        icon(
            "tc-warning",
            stroke = "M12 4.5 L21 19.5 L3 19.5 Z M12 10 L12 14",
            fill = "M11 16.9 a1 1 0 1 0 2 0 a1 1 0 1 0 -2 0 z",
        )
    }

    val Ticket: ImageVector by lazy {
        icon(
            "tc-ticket",
            stroke = "M6 6.5 h12 a2.5 2.5 0 0 1 2.5 2.5 v6 a2.5 2.5 0 0 1 -2.5 2.5 " +
                "h-12 a2.5 2.5 0 0 1 -2.5 -2.5 v-6 a2.5 2.5 0 0 1 2.5 -2.5 z " +
                "M14.5 7.5 L14.5 9.5 M14.5 12.1 L14.5 14.1",
        )
    }

    /** Plan B. */
    val AltRoute: ImageVector by lazy {
        icon(
            "tc-alt-route",
            stroke = "M6 20.5 L6 12 M6 12a6 6 0 0 1 6-6h5.5 M14.5 3 L18 6 L14.5 9",
        )
    }

    val Mic: ImageVector by lazy {
        icon(
            "tc-mic",
            stroke = "M12 3 h0 a3 3 0 0 1 3 3 v5 a3 3 0 0 1 -3 3 h0 a3 3 0 0 1 -3 -3 v-5 a3 3 0 0 1 3 -3 z " +
                "M5.5 12a6.5 6.5 0 0 0 13 0 M12 18.5 L12 21",
        )
    }

    val Play: ImageVector by lazy {
        icon("tc-play", fill = "M8 5.5 L19 12 L8 18.5 Z")
    }

    val Pause: ImageVector by lazy {
        icon(
            "tc-pause",
            fill = "M9.25 5 h0 a1.75 1.75 0 0 1 1.75 1.75 v10.5 a1.75 1.75 0 0 1 -3.5 0 " +
                "v-10.5 a1.75 1.75 0 0 1 1.75 -1.75 z " +
                "M14.75 5 h0 a1.75 1.75 0 0 1 1.75 1.75 v10.5 a1.75 1.75 0 0 1 -3.5 0 " +
                "v-10.5 a1.75 1.75 0 0 1 1.75 -1.75 z",
        )
    }

    val Map: ImageVector by lazy {
        icon(
            "tc-map",
            stroke = "M9 4 L3.8 5.8 v14 L9 18 l6 2 5.2-1.8 v-14 L15 6 Z " +
                "M9 4 L9 18 M15 6 L15 20",
        )
    }

    /** "Iniciar passeio". */
    val NearMe: ImageVector by lazy {
        icon("tc-near-me", stroke = "M20.5 3.5 L3.5 11 l7.4 2.1 L13 20.5 Z")
    }

    val Calendar: ImageVector by lazy {
        icon(
            "tc-calendar",
            stroke = "M7 5.5 h10 a3 3 0 0 1 3 3 v8.5 a3 3 0 0 1 -3 3 " +
                "h-10 a3 3 0 0 1 -3 -3 v-8.5 a3 3 0 0 1 3 -3 z " +
                "M4 10.5 L20 10.5 M8.5 3 L8.5 6 M15.5 3 L15.5 6",
        )
    }

    val Compass: ImageVector by lazy {
        icon(
            "tc-compass",
            stroke = "M3.5 12 a8.5 8.5 0 1 0 17 0 a8.5 8.5 0 1 0 -17 0 z " +
                "M14.8 9.2 L13.4 13.4 L9.2 14.8 L10.6 10.6 Z",
        )
    }

    val Wallet: ImageVector by lazy {
        icon(
            "tc-wallet",
            stroke = "M6.5 6 h11 a3 3 0 0 1 3 3 v7 a3 3 0 0 1 -3 3 " +
                "h-11 a3 3 0 0 1 -3 -3 v-7 a3 3 0 0 1 3 -3 z M3.5 10.5 L20.5 10.5",
            fill = "M15.2 14.8 a1.2 1.2 0 1 0 2.4 0 a1.2 1.2 0 1 0 -2.4 0 z",
        )
    }

    val MoreHoriz: ImageVector by lazy {
        icon(
            "tc-more-horiz",
            fill = "M3.9 12 a1.6 1.6 0 1 0 3.2 0 a1.6 1.6 0 1 0 -3.2 0 z " +
                "M10.4 12 a1.6 1.6 0 1 0 3.2 0 a1.6 1.6 0 1 0 -3.2 0 z " +
                "M16.9 12 a1.6 1.6 0 1 0 3.2 0 a1.6 1.6 0 1 0 -3.2 0 z",
        )
    }

    val Clock: ImageVector by lazy {
        icon(
            "tc-clock",
            stroke = "M4 12 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0 z M12 7.5 L12 12 L15.5 13.8",
        )
    }

    val Check: ImageVector by lazy {
        icon("tc-check", stroke = "M5 13 L9.5 17.5 L19 6.5")
    }

    /** Screen 07's 48dp "Encerrar passeio". */
    val Close: ImageVector by lazy {
        icon("tc-close", stroke = "M6 6 L18 18 M18 6 L6 18")
    }

    /** The "Ouvir juntos" action on screen 07. */
    /* -- converted for the operational half (screens 13 to 18) -------------- */

    /** Accommodation: the wallet's voucher row, and screen 16. */
    val Bed: ImageVector by lazy {
        icon(
            "tc-bed",
            stroke = "M3.5 20 L3.5 7 " +
                "M3.5 14 h14 a3 3 0 0 1 3 3 v3 " +
                "M3.5 20 L20.5 20 " +
                "M5.7 10.4 a2.1 2.1 0 1 0 4.2 0 a2.1 2.1 0 1 0 -4.2 0 z",
        )
    }

    /** Insurance and the papers that sit in the background of the trip. */
    val Shield: ImageVector by lazy {
        icon("tc-shield", stroke = "M12 3.5 L19 6 v6 c0 4.4 -3 7.6 -7 9 c-4 -1.4 -7 -4.6 -7 -9 V6 Z")
    }

    /** A telephone number that can be dialled. Screens 15, 16 and 17. */
    val Call: ImageVector by lazy {
        icon(
            "tc-call",
            stroke = "M6 3.5 h3 l1.6 4 l-2.1 1.5 a10 10 0 0 0 6.5 6.5 L16.5 13.4 l4 1.6 v3 " +
                "A2 2 0 0 1 18.4 20 C10.4 19.5 4.5 13.6 4 5.6 A2 2 0 0 1 6 3.5 Z",
        )
    }

    /** A document with no more specific icon of its own. */
    val Document: ImageVector by lazy {
        icon(
            "tc-document",
            stroke = "M13.5 3 H7 a2 2 0 0 0 -2 2 v14 a2 2 0 0 0 2 2 h10 a2 2 0 0 0 2 -2 V8.5 Z " +
                "M13.5 3 L13.5 8.5 L19 8.5 " +
                "M8.5 13 L15 13 M8.5 16.5 L13 16.5",
        )
    }

    /** The action that opens screen 14's QR mode. */
    val QrCode: ImageVector by lazy {
        icon(
            "tc-qr",
            stroke = "M5.5 4 h3.5 a1.5 1.5 0 0 1 1.5 1.5 v3.5 a1.5 1.5 0 0 1 -1.5 1.5 " +
                "h-3.5 a1.5 1.5 0 0 1 -1.5 -1.5 v-3.5 a1.5 1.5 0 0 1 1.5 -1.5 z " +
                "M15 4 h3.5 a1.5 1.5 0 0 1 1.5 1.5 v3.5 a1.5 1.5 0 0 1 -1.5 1.5 " +
                "h-3.5 a1.5 1.5 0 0 1 -1.5 -1.5 v-3.5 a1.5 1.5 0 0 1 1.5 -1.5 z " +
                "M5.5 13.5 h3.5 a1.5 1.5 0 0 1 1.5 1.5 v3.5 a1.5 1.5 0 0 1 -1.5 1.5 " +
                "h-3.5 a1.5 1.5 0 0 1 -1.5 -1.5 v-3.5 a1.5 1.5 0 0 1 1.5 -1.5 z",
            fill = "M13.5 13.5 h2.6 v2.6 h-2.6 z M17.4 17.4 h2.6 v2.6 h-2.6 z",
        )
    }

    /** Forward, where [ArrowLeft] is back. */
    val ArrowRight: ImageVector by lazy {
        icon("tc-arrow-right", stroke = "M4 12 L19 12 M13 6 L19 12 L13 18")
    }

    val Group: ImageVector by lazy {
        icon(
            "tc-group",
            stroke = "M6 9 a3.5 3.5 0 1 0 7 0 a3.5 3.5 0 1 0 -7 0 z " +
                "M3.5 19.5 a6 6 0 0 1 12 0 " +
                "M16.2 6.2 a3.5 3.5 0 0 1 0 5.6 " +
                "M17.6 14.7 a6 6 0 0 1 2.9 4.8",
        )
    }

    private fun icon(
        name: String,
        stroke: String? = null,
        fill: String? = null,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        if (stroke != null) {
            addPath(
                pathData = addPathNodes(stroke),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        if (fill != null) {
            addPath(
                pathData = addPathNodes(fill),
                fill = SolidColor(Color.Black),
            )
        }
    }.build()
}
