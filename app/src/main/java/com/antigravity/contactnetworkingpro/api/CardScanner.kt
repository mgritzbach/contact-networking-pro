package com.antigravity.contactnetworkingpro.api

import android.graphics.Bitmap
import com.antigravity.contactnetworkingpro.model.ContactDraft
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ScanResult(val draft: ContactDraft, val lines: List<String>)

object CardScanner {

    suspend fun scanBusinessCard(bitmap: Bitmap): Result<ScanResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val mlText = suspendCancellableCoroutine { cont ->
                    recognizer.process(image)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                    cont.invokeOnCancellation { recognizer.close() }
                }
                parse(mlText.text)
            }
        }

    private val emailRe   = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
    private val phoneRe   = Regex("[+\\d][\\d\\s\\-().]{5,}\\d")
    private val linkedinRe = Regex("linkedin\\.com/in/([^\\s/]+)", RegexOption.IGNORE_CASE)
    private val webRe     = Regex("(https?://|www\\.)[^\\s]+", RegexOption.IGNORE_CASE)

    private fun parse(raw: String): ScanResult {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

        val email       = emailRe.find(raw)?.value ?: ""
        val linkedinM   = linkedinRe.find(raw)
        val linkedinUrl = linkedinM?.let { "https://www.linkedin.com/in/${it.groupValues[1]}" } ?: ""
        val website     = webRe.findAll(raw)
            .map { it.value }
            .firstOrNull { !it.contains("linkedin", ignoreCase = true) } ?: ""

        val phones = phoneRe.findAll(raw)
            .map { it.value.trim() }
            .filter { it.replace(Regex("[^\\d]"), "").length >= 6 }
            .distinct()
            .toList()

        val metaLines = lines.filter { line ->
            !emailRe.containsMatchIn(line) &&
            !webRe.containsMatchIn(line) &&
            !phoneRe.containsMatchIn(line) &&
            line.length > 1
        }

        val draft = ContactDraft(
            fullName    = metaLines.getOrElse(0) { "" },
            jobTitle    = metaLines.getOrElse(1) { "" },
            company     = metaLines.getOrElse(2) { "" },
            phoneMobile = phones.getOrElse(0) { "" },
            phoneWork   = phones.getOrElse(1) { "" },
            email       = email,
            website     = website,
            linkedinUrl = linkedinUrl
        )
        return ScanResult(draft = draft, lines = lines)
    }
}
