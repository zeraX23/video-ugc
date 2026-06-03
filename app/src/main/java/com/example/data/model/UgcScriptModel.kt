package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class UgcScript(
    val title: String,
    val tagline: String,
    val targetAudience: String,
    val scenes: List<UgcScene>
) {
    companion object {
        /**
         * Safely parses UGC script JSON structure with bulletproof fallbacks.
         */
        fun fromJson(jsonString: String): UgcScript {
            // Clean up possible markdown code blocks, just in case they slipped through
            var cleanedJson = jsonString.trim()
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substringAfter("```json")
            } else if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substringAfter("```")
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substringBeforeLast("```")
            }
            cleanedJson = cleanedJson.trim()

            val root = JSONObject(cleanedJson)
            val title = root.optString("title", "Campagne UGC Baru")
            val tagline = root.optString("tagline", "Hook Promosi Keren")
            val targetAudience = root.optString("targetAudience", "Semua Pengguna")
            
            val scenesArray = root.optJSONArray("scenes")
            val scenesList = mutableListOf<UgcScene>()
            
            if (scenesArray != null) {
                for (i in 0 until scenesArray.length()) {
                    val sceneObj = scenesArray.getJSONObject(i)
                    scenesList.add(
                        UgcScene(
                            number = sceneObj.optInt("number", i + 1),
                            visual = sceneObj.optString("visual", "Scene visual tidak terdefinisi."),
                            voiceover = sceneObj.optString("voiceover", "Voiceover tidak terdefinisi."),
                            overlay = sceneObj.optString("overlay", "Teks overlay tidak terdefinisi."),
                            durationSecs = sceneObj.optInt("durationSecs", 4)
                        )
                    )
                }
            }

            // Fallback scene if empty
            if (scenesList.isEmpty()) {
                scenesList.add(
                    UgcScene(
                        number = 1,
                        visual = "Talent memegang produk di depan kamera dengan penuh antusias.",
                        voiceover = "Yuk cobain produk keren ini sekarang juga!",
                        overlay = "COBA SEKARANG JUGA!",
                        durationSecs = 5
                    )
                )
            }

            return UgcScript(title, tagline, targetAudience, scenesList)
        }
    }
}

data class UgcScene(
    val number: Int,
    val visual: String,
    val voiceover: String,
    val overlay: String,
    val durationSecs: Int
)
