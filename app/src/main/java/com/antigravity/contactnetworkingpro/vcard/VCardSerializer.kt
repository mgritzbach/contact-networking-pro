package com.antigravity.contactnetworkingpro.vcard

import com.antigravity.contactnetworkingpro.model.ContactDraft

/**
 * Generates conservative vCard 3.0 payloads that import consistently in Apple Contacts and
 * Android contact apps. vCard 3.0 requires VERSION, FN, and N, and requires CRLF line endings.
 */
object VCardSerializer {
    private const val CRLF = "\r\n"
    private val uriScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")

    fun serialize(contact: ContactDraft): String {
        val fullName = clean(contact.fullName)
        val company = clean(contact.company)
        val jobTitle = clean(contact.jobTitle)
        val mobile = clean(contact.phoneMobile)
        val workPhone = clean(contact.phoneWork)
        val email = clean(contact.email)
        val website = normalizeUrl(contact.website)
        val linkedIn = normalizeUrl(contact.linkedinUrl)
        val address = clean(contact.address)

        // FN must be present even when a user has not supplied a personal name.
        val formattedName = fullName.ifBlank {
            listOf(company, email, mobile, workPhone, jobTitle)
                .firstOrNull { it.isNotBlank() }
                ?: "Contact"
        }

        return buildList {
            add("BEGIN:VCARD")
            add("VERSION:3.0")
            add("N:${structuredName(fullName)}")
            add("FN:${escapeText(formattedName)}")
            if (company.isNotBlank()) add("ORG:${escapeText(company)}")
            if (jobTitle.isNotBlank()) add("TITLE:${escapeText(jobTitle)}")
            if (mobile.isNotBlank()) add("TEL;TYPE=CELL,VOICE:${escapeText(mobile)}")
            if (workPhone.isNotBlank()) add("TEL;TYPE=WORK,VOICE:${escapeText(workPhone)}")
            if (email.isNotBlank()) add("EMAIL;TYPE=INTERNET:${escapeText(email)}")
            if (website.isNotBlank()) add("URL:${escapeText(website)}")
            if (linkedIn.isNotBlank()) add("URL;TYPE=LINKEDIN:${escapeText(linkedIn)}")
            if (address.isNotBlank()) add("ADR:;;${escapeText(address)};;;;")
            add("END:VCARD")
        }.joinToString(separator = CRLF, postfix = CRLF)
    }

    /**
     * The editor currently collects one formatted-name field. Parse unambiguous two-part and
     * "Family, Given" names; keep more complex names intact in the given-name component rather
     * than guessing and corrupting a compound or non-Western family name.
     */
    private fun structuredName(fullName: String): String {
        if (fullName.isBlank()) return ";;;;"

        val commaParts = fullName.split(',', limit = 2).map(String::trim)
        if (commaParts.size == 2 && commaParts.all(String::isNotBlank)) {
            return "${escapeText(commaParts[0])};${escapeText(commaParts[1])};;;"
        }

        val words = fullName.split(Regex("\\s+")).filter(String::isNotBlank)
        return if (words.size == 2) {
            "${escapeText(words[1])};${escapeText(words[0])};;;"
        } else {
            ";${escapeText(fullName)};;;"
        }
    }

    private fun normalizeUrl(value: String): String {
        val trimmed = clean(value)
        if (trimmed.isBlank() || uriScheme.containsMatchIn(trimmed)) return trimmed
        return "https://$trimmed"
    }

    private fun clean(value: String): String = value.trim()

    private fun escapeText(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\r", "\\n")
        .replace("\n", "\\n")
}
