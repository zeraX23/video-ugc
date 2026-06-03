package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import com.example.data.model.UgcScript
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

object UgcExporter {
    private const val TAG = "UgcExporter"

    /**
     * Exports the UGC video script text to the device's public Downloads directory.
     */
    fun exportScriptToDownloads(context: Context, script: UgcScript): String? {
        val safeTitle = script.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "UGC_Script_$safeTitle.txt"
        val contentText = buildString {
            append("=========================================\n")
            append("        UGC VIDEO STUDIO CAMPAIGN        \n")
            append("=========================================\n\n")
            append("JUDUL VIDEO: ${script.title}\n")
            append("SLOGAN/HOOK: ${script.tagline}\n")
            append("TARGET AUDIENS: ${script.targetAudience}\n")
            append("SPESIFIKASI VIDEO: Resolusi 1080p (FHD), Rasio 9:16 (Vertical)\n\n")
            append("-----------------------------------------\n")
            append("                NASKAH VIDEO             \n")
            append("-----------------------------------------\n\n")
            for (scene in script.scenes) {
                append("SCENE ${scene.number} (${scene.durationSecs} detik)\n")
                append("[PROPERTI VISUAL]: ${scene.visual}\n")
                append("[TEKS POP-UP DI VIDEO]: ${scene.overlay}\n")
                append("[SUARA VOICEOVER]: \"${scene.voiceover}\"\n")
                append("-----------------------------------------\n\n")
            }
            append("Dibuat gratis tanpa token menggunakan UGC Video Studio AI.\n")
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        outputStream?.write(contentText.toByteArray())
                    }
                    "Berhasil mendownload naskah ke folder Downloads: $fileName"
                } else {
                    null
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(contentText.toByteArray())
                }
                "Berhasil mendownload naskah ke folder Downloads: $fileName"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting script text", e)
            null
        }
    }

    /**
     * Synthesizes the full UGC voiceover to a .wav audio file and saves it in public Downloads.
     */
    fun exportVoiceoverToDownloads(
        context: Context,
        script: UgcScript,
        tts: TextToSpeech,
        onStatusUpdate: (String) -> Unit
    ) {
        val safeTitle = script.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val audioFileName = "UGC_Voiceover_$safeTitle.wav"

        // Combine all scenes voiceover with minor pacing pause
        val combinedText = script.scenes.joinToString(separator = ". ... ") { it.voiceover }
        
        onStatusUpdate("Menyiapkan audio suara...")

        try {
            // Write to private cache folder first (which has actual File paths)
            val cacheFile = File(context.cacheDir, "temp_ugc_voiceover.wav")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }

            val utteranceId = "UGC_EXPORT_" + UUID.randomUUID().toString()
            
            // Set up UtteranceProgressListener to track when TTS finishes writing the file
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    Log.d(TAG, "TTS export started")
                }

                override fun onDone(id: String?) {
                    if (id == utteranceId) {
                        Log.d(TAG, "TTS export finished writing private file.")
                        
                        // Copy the written wav file from internal cache to public media store
                        val success = copyCacheToDownloads(context, cacheFile, audioFileName)
                        if (success) {
                            onStatusUpdate("Download Berhasil! Tersimpan di Downloads: $audioFileName")
                        } else {
                            onStatusUpdate("Gagal menyalin file audio ke Downloads.")
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    Log.e(TAG, "TTS export error: $id")
                    onStatusUpdate("Terjadi kesalahan rendering suara.")
                }

                override fun onError(id: String?, errorCode: Int) {
                    Log.e(TAG, "TTS export error: $id, code=$errorCode")
                    onStatusUpdate("Terjadi kesalahan rendering suara (Error $errorCode).")
                }
            })

            // Synthesize the file
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.synthesizeToFile(combinedText, null, cacheFile, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                tts.synthesizeToFile(combinedText, null, cacheFile.absolutePath)
                // Fallback invoke done since old API does not support utteranceId callback standard in TTS synthesize
                copyCacheToDownloads(context, cacheFile, audioFileName)
                onStatusUpdate("Download Berhasil! Tersimpan di Downloads: $audioFileName")
                TextToSpeech.SUCCESS
            }

            if (result != TextToSpeech.SUCCESS) {
                onStatusUpdate("Gagal memulai sintesis suara TTS.")
            } else {
                onStatusUpdate("Memproses penyusunan suara (synthesizing)...")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during voiceover export", e)
            onStatusUpdate("Gagal mendownload suara: ${e.localizedMessage}")
        }
    }

    /**
     * Copies our internal temporary cache wav file into public Media Store Downloads directory.
     */
    private fun copyCacheToDownloads(context: Context, srcFile: File, destName: String): Boolean {
        if (!srcFile.exists() || srcFile.length() == 0L) {
            Log.e(TAG, "Source file does not exist or empty")
            return false
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, destName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { out ->
                        if (out != null) {
                            FileInputStream(srcFile).use { input ->
                                input.copyTo(out)
                            }
                        }
                    }
                    srcFile.delete() // Clean up cache
                    true
                } else {
                    false
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, destName)
                FileInputStream(srcFile).use { input ->
                    FileOutputStream(destFile).use { out ->
                        input.copyTo(out)
                    }
                }
                srcFile.delete() // Clean up cache
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copyCacheToDownloads", e)
            false
        }
    }
}
