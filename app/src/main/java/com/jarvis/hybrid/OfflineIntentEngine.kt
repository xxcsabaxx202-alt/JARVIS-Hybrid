package com.jarvis.hybrid

import java.text.Normalizer
import java.util.Locale

/**
 * Deterministic, fully offline intent routing.
 * This is NOT a neural network or a replacement for llama.cpp inference.
 * It keeps common commands working while the native GGUF runtime is developed.
 */
object OfflineIntentEngine {
    enum class Intent { TIME, DATE, BATTERY, DIAGNOSTICS, WEATHER_DEBRECEN, UNKNOWN }

    fun classify(input: String): Intent {
        val normalized = Normalizer.normalize(input.lowercase(Locale.forLanguageTag("hu-HU")), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val words = normalized.split(" ").filter { it.isNotBlank() }.toSet()
        if (normalized.contains("debrecen") &&
            (normalized.contains("idojaras") || normalized.contains("hany fok") ||
                normalized.contains("esni fog") || normalized.contains("milyen ido") ||
                normalized.contains("varhato ido"))) return Intent.WEATHER_DEBRECEN
        if (words.any { it in setOf("akku", "akkumulator", "akkumulatorom", "toltottseg", "toltottség") }) return Intent.BATTERY
        if (normalized.contains("rendszerdiagnosztika") ||
            normalized.contains("rendszer allapota") ||
            normalized.contains("telefon allapota")) return Intent.DIAGNOSTICS
        if (normalized.contains("milyen datum") || normalized.contains("mai datum") ||
            normalized.contains("hanyadika") || normalized.contains("milyen nap")) return Intent.DATE
        if (normalized.contains("mennyi az ido") || normalized.contains("hany ora") ||
            normalized.contains("pontos ido") || normalized == "ido") return Intent.TIME
        return Intent.UNKNOWN
    }
}
