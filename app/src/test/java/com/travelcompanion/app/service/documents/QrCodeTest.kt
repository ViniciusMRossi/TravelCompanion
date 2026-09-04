package com.travelcompanion.app.service.documents

import com.google.zxing.BinaryBitmap
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.LuminanceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The only verification a QR can get without a camera: encode it, then read
 * it back.
 *
 * A code that does not scan fails the way a microphone that does not capture
 * fails — silently, and at the counter. A camera pointed at a real screen is
 * the verification that matters and it needs real ticket data to point at
 * (D061); this is what can be proved in the meantime, and it proves the part
 * that is ours: that the matrix carries the text.
 */
class QrCodeTest {

    private fun decode(matrix: QrMatrix): String {
        val bits = BitMatrix(matrix.size, matrix.size)
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.isDark(x, y)) bits.set(x, y)
            }
        }
        val source: LuminanceSource = BitMatrixLuminanceSource(bits)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    @Test
    fun `a locator survives the round trip`() {
        val matrix = encodeQr("BK7X2P9Q")!!
        assertEquals("BK7X2P9Q", decode(matrix))
    }

    /** Real ticket payloads are longer than a locator and carry punctuation. */
    @Test
    fun `an encoded ticket payload survives the round trip`() {
        val payload = "TKT|SJJ-MOS|2026-09-21T19:30|2|BK7X2P9Q"
        val matrix = encodeQr(payload)!!
        assertEquals(payload, decode(matrix))
    }

    @Test
    fun `accents survive, because titles and names have them`() {
        val matrix = encodeQr("Vinícius · Érika")!!
        assertEquals("Vinícius · Érika", decode(matrix))
    }

    @Test
    fun `nothing to encode is nothing, not an empty square`() {
        assertNull(encodeQr(""))
        assertNull(encodeQr("   "))
    }

    @Test
    fun `the matrix is square and has dark modules`() {
        val matrix = encodeQr("BK7X2P9Q")!!
        assertTrue(matrix.size > 0)
        var dark = 0
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) if (matrix.isDark(x, y)) dark++
        }
        assertTrue("a code with no dark modules is a white square", dark > 0)
    }
}

/** Minimal `LuminanceSource` over a bit matrix, so the reader can be used. */
private class BitMatrixLuminanceSource(
    private val bits: BitMatrix,
) : LuminanceSource(bits.width, bits.height) {

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val out = if (row != null && row.size >= width) row else ByteArray(width)
        for (x in 0 until width) out[x] = if (bits.get(x, y)) 0 else 0xFF.toByte()
        return out
    }

    override fun getMatrix(): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) out[y * width + x] = if (bits.get(x, y)) 0 else 0xFF.toByte()
        }
        return out
    }
}
