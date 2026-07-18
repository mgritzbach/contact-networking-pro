package com.antigravity.contactnetworkingpro.qr

import android.graphics.Bitmap
import android.graphics.Color

fun createQrBitmap(content: String, size: Int = 900): Bitmap {
    val matrix = QrCodeSpec.encode(content, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val offset = y * size
        for (x in 0 until size) {
            pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
