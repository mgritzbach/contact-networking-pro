package com.antigravity.contactnetworkingpro.model

data class ContactDraft(
    val fullName: String    = "",
    val jobTitle: String    = "",
    val company: String     = "",
    val phoneMobile: String = "",
    val phoneWork: String   = "",
    val email: String       = "",
    val website: String     = "",
    val linkedinUrl: String = "",
    val address: String     = ""
)

val ContactDraftSaver = androidx.compose.runtime.saveable.listSaver<ContactDraft, String>(
    save    = { listOf(it.fullName, it.jobTitle, it.company, it.phoneMobile, it.phoneWork, it.email, it.website, it.linkedinUrl, it.address) },
    restore = { ContactDraft(
        fullName    = it.getOrElse(0) { "" },
        jobTitle    = it.getOrElse(1) { "" },
        company     = it.getOrElse(2) { "" },
        phoneMobile = it.getOrElse(3) { "" },
        phoneWork   = it.getOrElse(4) { "" },
        email       = it.getOrElse(5) { "" },
        website     = it.getOrElse(6) { "" },
        linkedinUrl = it.getOrElse(7) { "" },
        address     = it.getOrElse(8) { "" }
    )}
)
