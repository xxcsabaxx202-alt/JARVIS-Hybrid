package com.jarvis.hybrid

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Bounded, locally stored conversation history. No background collection. */
class ConversationMemory(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_conversation", Context.MODE_PRIVATE)
    private val maxTurns = 12

    fun messages(): JSONArray {
        val saved = prefs.getString("history", "[]") ?: "[]"
        return try { JSONArray(saved) } catch (_: Exception) { JSONArray() }
    }

    fun add(role: String, message: String) {
        val current = messages()
        current.put(JSONObject().put("role", role).put("text", message.take(4000)))
        val start = (current.length() - maxTurns).coerceAtLeast(0)
        val trimmed = JSONArray()
        for (i in start until current.length()) trimmed.put(current.getJSONObject(i))
        prefs.edit().putString("history", trimmed.toString()).apply()
    }

    fun clear() = prefs.edit().remove("history").apply()

    fun summary(): String {
        val entries = messages()
        if (entries.length() == 0) return "Még nincs mentett AI-beszélgetés."
        return buildString {
            for (i in 0 until entries.length()) {
                val item = entries.optJSONObject(i) ?: continue
                append(if (item.optString("role") == "user") "Te: " else "JARVIS: ")
                append(item.optString("text").take(180))
                append("\n")
            }
        }.trim()
    }
}
