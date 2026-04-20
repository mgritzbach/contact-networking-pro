package com.antigravity.contactnetworkingpro.model

data class ContactDraft(
    val fullName: String    = "",
    val jobTitle: String    = "",
    val company: String     = "",
    val phone: String       = "",
    val email: String       = "",
    val website: String     = "",
    val linkedinUrl: String = "",
    val address: String     = ""
)

val ContactDraftSaver = androidx.compose.runtime.saveable.listSaver<ContactDraft, String>(
    save    = { listOf(it.fullName, it.jobTitle, it.company, it.phone, it.email, it.website, it.linkedinUrl, it.address) },
    restore = { ContactDraft(
        fullName    = it.getOrElse(0) { "" },
        jobTitle    = it.getOrElse(1) { "" },
        company     = it.getOrElse(2) { "" },
        phone       = it.getOrElse(3) { "" },
        email       = it.getOrElse(4) { "" },
        website     = it.getOrElse(5) { "" },
        linkedinUrl = it.getOrElse(6) { "" },
        address     = it.getOrElse(7) { "" }
    )}
)
