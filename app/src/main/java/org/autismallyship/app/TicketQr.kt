package org.autismallyship.app

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

// Error correction M recovers roughly 15 percent of a damaged code against level L's 7, which is
// the difference between a ticket that scans on a cracked or smudged screen and one that does not.
// The quiet zone is the blank margin a scanner uses to find the code in the first place.
//
// Black on white always, never theme colours. A QR on a dark background does not scan at all, so
// dark mode and sensory mode make no difference to this one bitmap.
fun ticketQrBitmap(content: String, sizePx: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )

    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val pixels = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) {
        val rowStart = y * matrix.width
        for (x in 0 until matrix.width) {
            pixels[rowStart + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }

    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}

private const val QUIET_ZONE_MODULES = 4
