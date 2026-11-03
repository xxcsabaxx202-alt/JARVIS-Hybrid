package com.jarvis.hybrid

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Stage 1 of on-device llama.cpp integration.
 * Manages a user-provided GGUF model privately on the Android device.
 * Inference is NOT enabled until a compatible native llama.cpp runtime is packaged.
 */
class LocalModelManager(private val context: Context) {
    private val modelFile: File get() = File(context.filesDir, "jarvis-model.gguf")

    fun isInstalled(): Boolean = modelFile.isFile && modelFile.length() > 1024L * 1024L
    fun sizeMb(): Long = if (modelFile.exists()) modelFile.length() / (1024L * 1024L) else 0L

    /** Call on a background thread. Copy to a temp file to avoid partially installed models. */
    fun importModel(uri: Uri): Long {
        val temp = File(context.filesDir, "jarvis-model.tmp")
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Nem olvasható a kiválasztott fájl.")
            stream.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            if (temp.length() < 1024L * 1024L) {
                throw IllegalArgumentException("A GGUF modell túl kicsi vagy sérült.")
            }
            val header = ByteArray(4)
            temp.inputStream().use { input ->
                if (input.read(header) != 4 || !header.contentEquals(byteArrayOf(71, 71, 85, 70))) {
                    throw IllegalArgumentException("A fájl nem GGUF formátumú.")
                }
            }
            if (modelFile.exists() && !modelFile.delete()) {
                throw IllegalStateException("A korábbi modell nem cserélhető.")
            }
            if (!temp.renameTo(modelFile)) {
                throw IllegalStateException("Nem sikerült elmenteni a modellt.")
            }
            return sizeMb()
        } finally {
            temp.delete()
        }
    }
}
