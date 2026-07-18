package com.antigravity.contactnetworkingpro.qr

import com.antigravity.contactnetworkingpro.data.ContactStorage
import com.antigravity.contactnetworkingpro.vcard.VCardSerializer

object QrContentRepository {
    fun contentFor(storage: ContactStorage, panelId: String): String =
        if (panelId == "contact") {
            val contact = storage.loadContact()
            if (contact.hasShareableContent()) VCardSerializer.serialize(contact) else ""
        } else {
            storage.loadPanelQrContent(panelId)
        }

    private fun com.antigravity.contactnetworkingpro.model.ContactDraft.hasShareableContent() =
        listOf(
            fullName, jobTitle, company, phoneMobile, phoneWork,
            email, website, linkedinUrl, address
        ).any(String::isNotBlank)
}
