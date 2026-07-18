package com.antigravity.contactnetworkingpro.vcard

import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardSerializerTest {
    @Test
    fun `serializes Michael as a standards-compliant Apple and Android contact`() {
        val payload = VCardSerializer.serialize(
            ContactDraft(
                fullName = " Michael Gritzbach ",
                jobTitle = "MPA'26 / MAM'21 / YCA'21",
                company = "Harvard / LBS / BeiDa",
                phoneMobile = "+18577069819",
                phoneWork = "+491783246826",
                email = "mic.gritzbach@gmail.com",
                website = " Michael-Gritzbach.eu "
            )
        )

        assertTrue(payload.contains("\r\nN:Gritzbach;Michael;;;\r\n"))
        assertTrue(payload.contains("\r\nFN:Michael Gritzbach\r\n"))
        assertTrue(payload.contains("TEL;TYPE=CELL,VOICE:+18577069819"))
        assertTrue(payload.contains("TEL;TYPE=WORK,VOICE:+491783246826"))
        assertTrue(payload.contains("URL:https://Michael-Gritzbach.eu"))
        assertTrue(payload.endsWith("END:VCARD\r\n"))
        assertFalse(payload.replace("\r\n", "").contains('\n'))
    }

    @Test
    fun `preserves complex formatted names without guessing family name`() {
        val payload = VCardSerializer.serialize(ContactDraft(fullName = "María del Mar García"))

        assertTrue(payload.contains("\r\nN:;María del Mar García;;;\r\n"))
        assertTrue(payload.contains("\r\nFN:María del Mar García\r\n"))
    }

    @Test
    fun `supports explicit family comma given syntax`() {
        val payload = VCardSerializer.serialize(ContactDraft(fullName = "Gritzbach, Michael"))

        assertTrue(payload.contains("\r\nN:Gritzbach;Michael;;;\r\n"))
        assertTrue(payload.contains("\r\nFN:Gritzbach\\, Michael\r\n"))
    }

    @Test
    fun `escapes text and always includes required name properties`() {
        val payload = VCardSerializer.serialize(
            ContactDraft(company = "Example, Inc.; Europe", address = "Line 1\nLine 2")
        )

        assertTrue(payload.contains("\r\nN:;;;;\r\n"))
        assertTrue(payload.contains("\r\nFN:Example\\, Inc.\\; Europe\r\n"))
        assertTrue(payload.contains("ADR:;;Line 1\\nLine 2;;;;"))
    }

    @Test
    fun `leaves absolute URL schemes unchanged`() {
        val payload = VCardSerializer.serialize(
            ContactDraft(fullName = "Michael Gritzbach", website = "https://example.com/profile")
        )

        assertEquals(1, Regex("URL:https://example.com/profile").findAll(payload).count())
    }

    @Test
    fun `QR encoding and decoding preserves the vCard byte for byte`() {
        val payload = VCardSerializer.serialize(
            ContactDraft(
                fullName = "Michael Gritzbach",
                company = "Harvard / LBS / BeiDa",
                phoneMobile = "+18577069819"
            )
        )
        val size = 900
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val decoded = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(RGBLuminanceSource(size, size, pixels)))
        ).text

        assertEquals(payload, decoded)
    }
}
