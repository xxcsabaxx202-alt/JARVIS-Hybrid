package com.jarvis.hybrid

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineIntentEngineTest {
    @Test fun hungarianWeatherVariants() {
        val cases = listOf(
            "Milyen lesz ma az aktuális időjárás Debrecenben?",
            "Milyen lesz ma az idojaras Debrecenben?",
            "Hány fok van Debrecenben?",
            "Esni fog Debrecenben?",
            "Milyen idő várható Debrecenben?"
        )
        cases.forEach {
            assertEquals(it, OfflineIntentEngine.Intent.WEATHER_DEBRECEN,
                OfflineIntentEngine.classify(it))
        }
    }

    @Test fun timeAndDate() {
        assertEquals(OfflineIntentEngine.Intent.TIME,
            OfflineIntentEngine.classify("Mennyi az idő?"))
        assertEquals(OfflineIntentEngine.Intent.TIME,
            OfflineIntentEngine.classify("Hány óra van?"))
        assertEquals(OfflineIntentEngine.Intent.DATE,
            OfflineIntentEngine.classify("Milyen dátum van ma?"))
    }

    @Test fun doNotHijackNormalQuestions() {
        assertEquals(OfflineIntentEngine.Intent.UNKNOWN,
            OfflineIntentEngine.classify("Mi az idő relativitáselmélete?"))
        assertEquals(OfflineIntentEngine.Intent.UNKNOWN,
            OfflineIntentEngine.classify("Írj egy mesét Debrecenről"))
    }
}
