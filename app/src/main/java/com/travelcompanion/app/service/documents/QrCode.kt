package com.travelcompanion.app.service.documents

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * A QR code as a matrix of modules, with no Android in it.
 *
 * Encoding only. Nothing here reads a code, asks for a camera or touches the
 * network; the result is a grid of booleans that the screen paints. Kept off
 * the Android types so it can be round-tripped in a unit test — encoded here
 * and decoded back — which is the only verification a QR can get without a
 * camera pointed at a screen (D061).
 */
data class QrMatrix(val size: Int, private val modules: BooleanArray) {
    fun isDark(x: Int, y: Int): Boolean = modules[y * size + x]

    // Data classes over arrays compare by reference; the screen only ever
    // reads this, so identity is the honest answer rather than a wrong one.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * Encodes [text] as a QR, or null when it cannot be encoded.
 *
 * Error correction M: enough redundancy for a phone screen read at an angle,
 * without the density that makes a small code hard to acquire.
 */
fun encodeQr(text: String): QrMatrix? {
    if (text.isBlank()) return null
    val matrix: BitMatrix = runCatching {
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            SIZE,
            SIZE,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to QUIET_ZONE,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
    }.getOrNull() ?: return null

    val modules = BooleanArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            modules[y * matrix.width + x] = matrix.get(x, y)
        }
    }
    return QrMatrix(size = matrix.width, modules = modules)
}

/** Module count of the encoded grid, before it is scaled to the screen. */
private const val SIZE = 512

/** Four modules, which is what the QR specification asks for. */
private const val QUIET_ZONE = 4
