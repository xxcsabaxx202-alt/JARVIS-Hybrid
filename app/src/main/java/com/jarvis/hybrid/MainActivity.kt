package com.jarvis.hybrid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("DEPRECATION")
class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val hungarian = Locale("hu", "HU")
    private lateinit var output: TextView
    private lateinit var commandInput: EditText
    private var speech: TextToSpeech? = null
    private val speechRequest = 101
    private val micRequest = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speech = TextToSpeech(this, this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 28)
        }
        val title = TextView(this).apply {
            text = "JARVIS Hybrid v0.1"
            textSize = 26f
        }
        output = TextView(this).apply {
            text = "Üdvözöllek! Írj egy parancsot, vagy nyomd meg a mikrofont."
            textSize = 18f
            setPadding(0, 24, 0, 24)
        }
        commandInput = EditText(this).apply {
            hint = "Például: jegyezd meg: tej"
            setSingleLine(false)
        }
        val send = Button(this).apply {
            text = "Küldés"
            setOnClickListener { handle(commandInput.text.toString()) }
        }
        val mic = Button(this).apply {
            text = "🎤 Beszéd"
            setOnClickListener { startListening() }
        }
        val scroll = ScrollView(this)
        scroll.addView(output)
        layout.addView(title)
        layout.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        layout.addView(commandInput)
        layout.addView(send)
        layout.addView(mic)
        setContentView(layout)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hu-HU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Beszélj JARVIS-hoz")
            }
            startActivityForResult(intent, speechRequest)
        } catch (_: Exception) {
            reply("Nem található beszédfelismerő szolgáltatás. Használd a szöveges parancsokat.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequest && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (requestCode == micRequest) {
            reply("A mikrofon használatához engedély szükséges.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                commandInput.setText(spoken)
                handle(spoken)
            }
        }
    }

    private fun handle(raw: String) {
        val command = raw.trim()
        val lower = command.lowercase(hungarian)
        val prefs = getSharedPreferences("jarvis_notes", MODE_PRIVATE)
        when {
            command.isBlank() -> reply("Írj be egy parancsot.")
            lower.startsWith("jegyezd meg:") -> {
                val note = command.substringAfter(":").trim()
                if (note.isBlank()) reply("Mit jegyezzek meg?")
                else {
                    val old = prefs.getString("notes", "") ?: ""
                    prefs.edit().putString("notes", (old + "\n" + note).trim()).apply()
                    reply("Megjegyeztem: $note")
                }
            }
            lower.contains("mit jegyeztél meg") || lower.contains("jegyzeteim") -> {
                reply(prefs.getString("notes", "")?.takeIf { it.isNotBlank() } ?: "Még nincs jegyzeted.")
            }
            lower.contains("idő") || lower.contains("óra") -> reply(
                "Most " + SimpleDateFormat("HH:mm", hungarian).format(Date()) + " van."
            )
            lower.contains("dátum") || lower.contains("mai nap") -> reply(
                "Ma " + SimpleDateFormat("yyyy. MMMM d., EEEE", hungarian).format(Date()) + " van."
            )
            lower.contains("szia") || lower.contains("helló") -> reply("Szia! JARVIS készen áll.")
            else -> reply("Ezt a parancsot még nem ismerem. Az online AI egy későbbi verzióban érkezik.")
        }
    }

    private fun reply(message: String) {
        output.text = message
        speech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) speech?.language = hungarian
    }

    override fun onDestroy() {
        speech?.stop()
        speech?.shutdown()
        super.onDestroy()
    }
}
