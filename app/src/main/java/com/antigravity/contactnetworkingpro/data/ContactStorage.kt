package com.antigravity.contactnetworkingpro.data

import android.content.Context
import com.antigravity.contactnetworkingpro.model.ContactDraft

class ContactStorage(context: Context) {
    private val prefs = context.getSharedPreferences("cnp_store", Context.MODE_PRIVATE)

    fun loadContact() = ContactDraft(
        fullName    = prefs.getString("c_name", "")      ?: "",
        jobTitle    = prefs.getString("c_title", "")     ?: "",
        company     = prefs.getString("c_company", "")   ?: "",
        phone       = prefs.getString("c_phone", "")     ?: "",
        email       = prefs.getString("c_email", "")     ?: "",
        website     = prefs.getString("c_website", "")   ?: "",
        linkedinUrl = prefs.getString("c_linkedin", "")  ?: "",
        address     = prefs.getString("c_address", "")   ?: ""
    )

    fun saveContact(d: ContactDraft) {
        prefs.edit()
            .putString("c_name",    d.fullName)
            .putString("c_title",   d.jobTitle)
            .putString("c_company", d.company)
            .putString("c_phone",   d.phone)
            .putString("c_email",   d.email)
            .putString("c_website", d.website)
            .putString("c_linkedin",d.linkedinUrl)
            .putString("c_address", d.address)
            .apply()
    }

    fun loadLinkedinUri(): String = prefs.getString("linkedin_uri", "") ?: ""
    fun saveLinkedinUri(uri: String) = prefs.edit().putString("linkedin_uri", uri).apply()

    fun loadWhatsappUri(): String = prefs.getString("whatsapp_uri", "") ?: ""
    fun saveWhatsappUri(uri: String) = prefs.edit().putString("whatsapp_uri", uri).apply()
}
