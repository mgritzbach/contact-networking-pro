package com.antigravity.contactnetworkingpro.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Test

class QrCodeSpecTest {
    @Test
    fun unicodePayloadRoundTripsAsUtf8() {
        val payload = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:李明 🚀\r\nORG:مؤسسة عالمية\r\nEND:VCARD\r\n"
        val matrix = QrCodeSpec.encode(payload, 640)
        val pixels = IntArray(640 * 640) { index ->
            if (matrix[index % 640, index / 640]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(640, 640, pixels)))
        val decoded = QRCodeReader().decode(
            bitmap,
            mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")
        ).text

        assertEquals(payload, decoded)
    }
}
