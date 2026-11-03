package com.jarvis.hybrid

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** API key is encrypted at rest. It is never committed to the repository. */
class GeminiClient(private val context: Context) {
    private val alias = "jarvis_gemini_key_v1"
    private val prefs = context.getSharedPreferences("jarvis_ai_settings", Context.MODE_PRIVATE)

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun saveKey(key: String) {
        val value = key.trim()
        if (value.isBlank()) {
            prefs.edit().remove("key").remove("iv").apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString("key", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    fun hasKey(): Boolean = prefs.contains("key")

    private fun readKey(): String {
        val encrypted = prefs.getString("key", null) ?: return ""
        val iv = prefs.getString("iv", null) ?: return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    /** List supported text-generation models for the user's key. */
    private fun availableModels(key: String): List<String> {
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("x-goog-api-key", key)
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Modelllista HTTP $code: " +
                runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }
                    .getOrNull().orEmpty().take(140))
            val models = JSONObject(raw).optJSONArray("models") ?: JSONArray()
            val available = mutableListOf<String>()
            for (i in 0 until models.length()) {
                val item = models.optJSONObject(i) ?: continue
                val methods = item.optJSONArray("supportedGenerationMethods") ?: continue
                var generates = false
                for (j in 0 until methods.length()) {
                    if (methods.optString(j) == "generateContent") generates = true
                }
                if (generates) {
                    val name = item.optString("name").removePrefix("models/")
                    if (name.startsWith("gemini-") && !name.contains("image") &&
                        !name.contains("tts") && !name.contains("embedding") &&
                        !name.contains("robotics") && !name.contains("computer-use")) {
                        available.add(name)
                    }
                }
            }
            val preferred = listOf("gemini-2.5-flash-lite", "gemini-3-flash-preview",
                "gemini-3.8-flash", "gemini-3-flash", "gemini-2.5-flash")
            return available.distinct().sortedWith(
                compareBy<String> { name ->
                    val index = preferred.indexOf(name)
                    if (index < 0) 100 else index
                }.thenBy { it }
            ).take(5)
        } finally {
            connection.disconnect()
        }
    }

    /** Must run off the Android main thread. */
    fun ask(question: String, history: JSONArray = JSONArray()): String {
        val key = readKey()
        if (key.isBlank()) throw IllegalStateException("Nincs beállítva Gemini API-kulcs.")
        val models = availableModels(key)
        if (models.isEmpty()) throw IllegalStateException("Nincs elérhető Gemini szövegmodell.")
        var lastError = "Ismeretlen hiba"
        for (model in models) {
            for (attempt in 0..1) {
                try {
                    return requestAnswer(key, model, question, history)
                } catch (e: GeminiHttpException) {
                    lastError = "Gemini HTTP ${e.status}: ${e.message.orEmpty().take(130)}"
                    if (e.status == 429) {
                        throw IllegalStateException("A Gemini-kvóta elfogyott vagy túl sok kérés érkezett. Próbáld később. " + lastError)
                    }
                    if (e.status == 401 || e.status == 403) {
                        throw IllegalStateException("Ellenőrizd a Gemini API-kulcsot és a hozzáférést. " + lastError)
                    }
                    if (e.status != 503 && e.status != 502 && e.status != 500 && e.status != 404) {
                        throw IllegalStateException(lastError)
                    }
                    if (attempt == 0 && e.status != 404) Thread.sleep(900L)
                    if (e.status == 404) break
                }
            }
        }
        throw IllegalStateException("A Gemini-modellek átmenetileg nem válaszolnak. " + lastError)
    }

    private class GeminiHttpException(val status: Int, message: String) : Exception(message)

    private fun buildContents(history: JSONArray, question: String): JSONArray {
        val contents = JSONArray()
        for (i in 0 until history.length()) {
            val item = history.optJSONObject(i) ?: continue
            val role = if (item.optString("role") == "model") "model" else "user"
            val text = item.optString("text").take(4000)
            if (text.isNotBlank()) {
                contents.put(JSONObject().put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", text))))
            }
        }
        contents.put(JSONObject().put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text", question))))
        return contents
    }

    private fun requestAnswer(key: String, model: String, question: String, history: JSONArray): String {
        val connection = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("x-goog-api-key", key)
            val payload = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", "JARVIS vagy, egy segítőkész magyar nyelvű Android-asszisztens. Válaszolj tömören magyarul. Ne állítsd, hogy hozzáférsz a telefon adataihoz vagy műveleteihez, ha nem tudsz.")
                )))
                .put("contents", buildContents(history, question))
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(response).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                throw GeminiHttpException(status, message)
            }
            val candidates = JSONObject(response).optJSONArray("candidates")
            val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            val answer = buildString {
                if (parts != null) for (i in 0 until parts.length()) {
                    val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (text.isNotBlank()) append(text)
                }
            }.trim()
            return answer.ifBlank { "A modell most nem adott szöveges választ." }
        } finally {
            connection.disconnect()
        }
    }
}
