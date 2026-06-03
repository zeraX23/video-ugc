package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    // OkHttpClient with generous timeouts as recommended by the gemini-api skill rules
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Generates a structural UGC campaign script using Gemini.
     */
    suspend fun generateUgcScript(
        category: String,
        style: String,
        userPromptText: String,
        base64Image: String? = null,
        imageMimeType: String = "image/jpeg"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is empty or placeholder!")
            throw IllegalStateException("API Key belum dikonfigurasi di AI Studio. Harap hubungkan Secrets di panel.")
        }

        // Deep System Instructions describing how to behave, style elements, and structure JSON
        val systemInstruction = """
            Anda adalah seorang scriptwriter expert dan produser video UGC (User Generated Content) profesional untuk TikTok, Instagram Reels, dan YouTube Shorts.
            Tugas Anda adalah memformulasikan naskah video UGC promosi / pemasaran yang berpotensi viral berdurasi 15-30 detik berdasarkan masukan berikut:
            - Kategori: $category
            - Gaya/Style Konten: $style
            - Skenario/Ide: $userPromptText
            - Gambar Produk: ${if (base64Image != null) "Telah dilampirkan visual produk. Analisis gambar tersebut secara detail dan masukkan poin-poin visual produk ke dalam naskah!" else "Tidak ada gambar"}

            Naskah harus mengandung HOOK 3 detik pertama yang sangat mencuri perhatian, penjelasan manfaat utama (Body), dan Call to Action (CTA) yang natural.
            Gunakan gaya bahasa Indonesia yang kasual, otentik (seperti diucapkan orang biasa, bukan iklan TV formal), relatable, dan menarik.

            RESPON HARUS BERUPA JSON OBJEK MURNI yang valid tanpa pembungkus seperti ```json atau ```.
            Format JSON harus sama persis dengan skema ini:
            {
              "title": "Judul Unik Video UGC",
              "tagline": "Hook Utama / Tagline utama",
              "targetAudience": "Deskripsi singkat target audiens produk",
              "scenes": [
                {
                  "number": 1,
                  "visual": "Aktivitas visual talent (misal: Ceritakan apa yang terlihat di kamera secara detail, sudut pandang kamera, ekspresi talent)",
                  "voiceover": "Kata-kata lisan/suara yang diucapkan oleh talent secara kasual indonesia",
                  "overlay": "Teks teks teks pop-up yang wajib muncul di layar video (1-5 kata, kapital, menarik)",
                  "durationSecs": 4
                }
              ]
            }
            Buatlah minimal 4 scene (Scene 1: Hook, Scene 2 & 3: Problem/Solution body, Scene 4: CTA). Jawablah HANYA objek JSON tersebut.
        """.trimIndent()

        // Construct standard REST request payload for Gemini API
        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        if (base64Image != null) {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", imageMimeType)
                                    put("data", base64Image)
                                })
                            })
                        }
                        put(JSONObject().apply {
                            put("text", "Buatkan naskah video UGC baru.")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val requestBody = requestJson.toString().toRequestBody(mediaType)
        val url = "$BASE_URL?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Response failed: code=${response.code} error=$bodyString")
                    val errJson = try { JSONObject(bodyString) } catch (e: Exception) { null }
                    val errMsg = errJson?.optJSONObject("error")?.optString("message") ?: "Kegagalan jaringan API"
                    throw IOException("Kesalahan API (${response.code}): $errMsg")
                }

                // Analyze returned candidates
                val parsedRoot = JSONObject(bodyString)
                val candidates = parsedRoot.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    throw IOException("Model tidak mengembalikan draf script UGC. Coba sesuaikan prompt Anda.")
                }

                val textOutput = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Return clean string
                textOutput.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generateUgcScript", e)
            throw e
        }
    }
}
