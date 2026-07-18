package com.antigravity.contactnetworkingpro.data

import android.content.Context
import com.antigravity.contactnetworkingpro.model.ContactDraft

class ContactStorage(context: Context) {
    private val prefs = context.getSharedPreferences("cnp_store", Context.MODE_PRIVATE)

    // ── Setup state ────────────────────────────────────────────────────────────
    fun isSetupComplete(): Boolean = prefs.getBoolean("setup_complete", false)
    fun markSetupComplete()       = prefs.edit().putBoolean("setup_complete", true).apply()

    fun getPanelCount(): Int                = prefs.getInt("panel_count", 3)
    fun savePanelCount(count: Int)          = prefs.edit().putInt("panel_count", count).apply()

    fun getCustomPanelName(slot: Int): String =
        prefs.getString("custom_name_$slot", "") ?: ""
    fun saveCustomPanelName(slot: Int, name: String) =
        prefs.edit().putString("custom_name_$slot", name).apply()

    // ── Contact ────────────────────────────────────────────────────────────────
    fun loadContact() = ContactDraft(
        fullName    = prefs.getString("c_name",         "") ?: "",
        jobTitle    = prefs.getString("c_title",        "") ?: "",
        company     = prefs.getString("c_company",      "") ?: "",
        phoneMobile = prefs.getString("c_phone_mobile", "") ?: "",
        phoneWork   = prefs.getString("c_phone_work",   "") ?: "",
        email       = prefs.getString("c_email",        "") ?: "",
        website     = prefs.getString("c_website",      "") ?: "",
        linkedinUrl = prefs.getString("c_linkedin",     "") ?: "",
        address     = prefs.getString("c_address",      "") ?: ""
    )

    fun saveContact(d: ContactDraft) {
        prefs.edit()
            .putString("c_name",         d.fullName)
            .putString("c_title",        d.jobTitle)
            .putString("c_company",      d.company)
            .putString("c_phone_mobile", d.phoneMobile)
            .putString("c_phone_work",   d.phoneWork)
            .putString("c_email",        d.email)
            .putString("c_website",      d.website)
            .putString("c_linkedin",     d.linkedinUrl)
            .putString("c_address",      d.address)
            .apply()
    }

    // ── QR content strings (panel id → decoded QR text) ───────────────────────
    fun loadPanelQrContent(panelId: String): String = prefs.getString("qr_$panelId", "") ?: ""
    fun savePanelQrContent(panelId: String, content: String) =
        prefs.edit().putString("qr_$panelId", content).apply()
}
