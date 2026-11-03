package com.jarvis.hybrid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
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
    private val hu = Locale("hu", "HU")
    private val cyan = Color.rgb(49, 221, 244)
    private val dark = Color.rgb(8, 17, 32)
    private val panel = Color.rgb(19, 35, 54)
    private lateinit var output: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private var tts: TextToSpeech? = null
    private val speechRequest = 101
    private val micRequest = 102

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, border: Int = cyan): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), border)
        }

    private fun label(text: String, size: Float, color: Int = Color.WHITE): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
        }

    private fun button(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 14f
            isAllCaps = false
            setTextColor(dark)
            background = rounded(cyan, cyan)
            setOnClickListener { action() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        tts = TextToSpeech(this, this)

        val screen = ScrollView(this).apply {
            setBackgroundColor(dark)
            fillViewport = true
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
        }
        screen.addView(body)

        body.addView(label("J A R V I S", 32f, cyan).apply {
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        })
        body.addView(label("HYBRID ASSISTANT  •  v0.2", 12f, Color.LTGRAY).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(28))
        })

        val core = label("◉", 100f, cyan).apply {
            gravity = Gravity.CENTER
            background = rounded(panel)
            setPadding(0, dp(10), 0, dp(10))
            contentDescription = "JARVIS vezérlőközpont"
        }
        body.addView(core, LinearLayout.LayoutParams(-1, dp(185)))

        status = label("● RENDSZER AKTÍV  •  ALAP PARANCSOK", 12f, cyan).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(18))
        }
        body.addView(status)

        val responsePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, Color.rgb(38, 79, 100))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        responsePanel.addView(label("JARVIS VÁLASZA", 11f, cyan).apply {
            setTypeface(null, Typeface.BOLD)
        })
        output = label("Üdvözöllek! Készen állok. Adj meg egy parancsot.", 17f).apply {
            setPadding(0, dp(12), 0, dp(5))
            minHeight = dp(90)
        }
        responsePanel.addView(output)
        body.addView(responsePanel)

        input = EditText(this).apply {
            hint = "Írj egy parancsot..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            background = rounded(panel, Color.rgb(38, 79, 100))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setSingleLine(false)
        }
        body.addView(input, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(20)
            bottomMargin = dp(12)
        })

        body.addView(button("🎙  BESZÉLJ JARVIS-HOZ") { listen() },
            LinearLayout.LayoutParams(-1, dp(56)))
        body.addView(button("➤  PARANCS KÜLDÉSE") { handle(input.text.toString()) },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })

        body.addView(label("GYORS PARANCSOK", 12f, cyan).apply {
            setPadding(0, dp(24), 0, dp(8))
            setTypeface(null, Typeface.BOLD)
        })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val options = listOf("🕒 Idő" to "Mennyi az idő?",
            "📅 Dátum" to "Mai dátum",
            "📝 Jegyzetek" to "Jegyzeteim")
        options.forEach { (title, command) ->
            shortcuts.addView(button(title) { handle(command) },
                LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                    marginEnd = dp(5)
                })
        }
        body.addView(shortcuts)
        body.addView(label("A beszédfelismerés a telefon szolgáltatását használja; internetet igényelhet. Az AI-csevegés később érkezik.", 12f, Color.LTGRAY).apply {
            setPadding(0, dp(20), 0, 0)
        })
        setContentView(screen)
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            return
        }
        try {
            status.text = "● HALLGATLAK..."
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == micRequest) {
            if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) listen()
            else reply("A mikrofon használatához engedély szükséges.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == speechRequest && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) {
                input.setText(words)
                handle(words)
            } else status.text = "● RENDSZER AKTÍV"
        } else if (requestCode == speechRequest) status.text = "● RENDSZER AKTÍV"
    }

    private fun handle(raw: String) {
        val command = raw.trim()
        val lower = command.lowercase(hu)
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
            lower.contains("mit jegyeztél meg") || lower.contains("jegyzeteim") ->
                reply(prefs.getString("notes", "")?.takeIf { it.isNotBlank() } ?: "Még nincs jegyzeted.")
            lower.contains("idő") || lower.contains("óra") ->
                reply("Most " + SimpleDateFormat("HH:mm", hu).format(Date()) + " van.")
            lower.contains("dátum") || lower.contains("mai nap") ->
                reply("Ma " + SimpleDateFormat("yyyy. MMMM d., EEEE", hu).format(Date()) + " van.")
            lower.contains("szia") || lower.contains("helló") ->
                reply("Szia! JARVIS készen áll.")
            else -> reply("Ezt a parancsot még nem ismerem. Az online AI egy későbbi verzióban érkezik.")
        }
    }

    private fun reply(message: String) {
        output.text = message
        status.text = "● RENDSZER AKTÍV"
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_response")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = hu
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
