package com.antigravity.contactnetworkingpro.api

import android.graphics.Bitmap
import android.util.Base64
import com.antigravity.contactnetworkingpro.model.ContactDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object ClaudeVisionClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun scanBusinessCard(bitmap: Bitmap, apiKey: String): Result<ContactDraft> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base64 = bitmap.toBase64Jpeg(maxDim = 1024)

                val body = JSONObject().apply {
                    put("model", "claude-sonnet-4-6")
                    put("max_tokens", 512)
                    put("messages", JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image")
                                    put("source", JSONObject().apply {
                                        put("type", "base64")
                                        put("media_type", "image/jpeg")
                                        put("data", base64)
                                    })
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", """
                                        Extract contact information from this business card.
                                        Return ONLY a valid JSON object — no markdown, no explanation — with exactly these keys:
                                        fullName, jobTitle, company, phone, email, website, linkedinUrl, address.
                                        Use an empty string "" for any field not found on the card.
                                    """.trimIndent())
                                })
                            })
                        }
                    ))
                }.toString()

                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = http.newCall(request).execute()
                val responseBody = response.body?.string() ?: error("Empty response")
                if (!response.isSuccessful) error("API error ${response.code}: $responseBody")

                val text = JSONObject(responseBody)
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                // Strip ```json fences if present
                val json = text
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```").trim()

                val obj = JSONObject(json)
                ContactDraft(
                    fullName    = obj.optString("fullName"),
                    jobTitle    = obj.optString("jobTitle"),
                    company     = obj.optString("company"),
                    phone       = obj.optString("phone"),
                    email       = obj.optString("email"),
                    website     = obj.optString("website"),
                    linkedinUrl = obj.optString("linkedinUrl"),
                    address     = obj.optString("address")
                )
            }
        }

    private fun Bitmap.toBase64Jpeg(maxDim: Int): String {
        val scaled = if (width > maxDim || height > maxDim) {
            val ratio = maxDim.toFloat() / maxOf(width, height)
            Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
        } else this
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
