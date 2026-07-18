package com.antigravity.contactnetworkingpro.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** One QR encoding contract shared by every surface in the app. */
object QrCodeSpec {
    private val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 4
    )

    fun encode(content: String, size: Int): BitMatrix {
        require(content.isNotBlank()) { "QR content must not be blank" }
        require(size > 0) { "QR size must be positive" }
        return QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    }
}
