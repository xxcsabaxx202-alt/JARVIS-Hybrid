package com.jarvis.hybrid

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import android.speech.tts.UtteranceProgressListener
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

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
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var ai: GeminiClient
    private lateinit var memory: ConversationMemory
    private lateinit var hud: JarvisCoreView
    private var pulse: ObjectAnimator? = null
    private var autoListen = false
    private var voicePitch = 0.78f
    private var voiceRate = 0.91f
    private var waitingForAi = false
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
        ai = GeminiClient(this)
        memory = ConversationMemory(this)
        val voicePrefs = getSharedPreferences("jarvis_voice", MODE_PRIVATE)
        voicePitch = voicePrefs.getFloat("pitch", 0.78f)
        voiceRate = voicePrefs.getFloat("rate", 0.91f)

        val screen = ScrollView(this).apply {
            setBackgroundColor(dark)
            isFillViewport = true
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
        body.addView(label("HYBRID ASSISTANT  •  v0.6", 12f, Color.LTGRAY).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(28))
        })

        hud = JarvisCoreView(this).apply {
            background = rounded(panel, Color.rgb(24, 106, 128))
        }
        body.addView(hud, LinearLayout.LayoutParams(-1, dp(240)))
        body.addView(label("◈  A R C   R E A C T O R   /   ONLINE", 11f, cyan).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(5))
        })

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
            setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                    sendTypedCommand()
                    true
                } else false
            }
        }
        body.addView(input, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(20)
            bottomMargin = dp(12)
        })

        body.addView(button("🎙  BESZÉLJ JARVIS-HOZ") { listen() },
            LinearLayout.LayoutParams(-1, dp(56)))
        body.addView(button("➤  PARANCS KÜLDÉSE") { sendTypedCommand() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })

        body.addView(button("🔁  BESZÉLGETŐ MÓD: KI/BE") {
            autoListen = !autoListen
            reply(if (autoListen) "Beszélgető mód bekapcsolva. A válasz után újra megnyitom a mikrofont. A leállításhoz nyomd meg újra a gombot." else "Beszélgető mód kikapcsolva.")
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })

        body.addView(button("🎚  JARVIS HANG BEÁLLÍTÁSA") { showVoiceSettings() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })

        body.addView(button("⚙  ONLINE AI BEÁLLÍTÁSA") { showAiSettings() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })

        body.addView(label("GYORS PARANCSOK", 12f, cyan).apply {
            setPadding(0, dp(24), 0, dp(8))
            setTypeface(null, Typeface.BOLD)
        })
        val shortcuts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val options = listOf("🕒 Idő" to "Mennyi az idő?",
            "📅 Dátum" to "Mai dátum",
            "📝 Memória" to "Beszélgetési előzmények")
        options.forEach { (title, command) ->
            shortcuts.addView(button(title) { handle(command) },
                LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                    marginEnd = dp(5)
                })
        }
        body.addView(shortcuts)
        body.addView(label("Példák: nyisd meg a beállításokat; tárcsázd: +36...; emlékeztess; beszélgetési előzmények; töröld a beszélgetést. Az automatikus újrahallgatás csak nyitott alkalmazásban működik.", 12f, Color.LTGRAY).apply {
            setPadding(0, dp(20), 0, 0)
        })
        setContentView(screen)
    }

    private fun sendTypedCommand() {
        val command = input.text.toString().trim()
        if (command.isBlank()) {
            reply("Írj be egy parancsot a küldés előtt.")
            input.requestFocus()
            return
        }
        input.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(input.windowToken, 0)
        input.setText("")
        handle(command)
    }

    private fun showVoiceSettings() {
        val labels = arrayOf("Mély filmes hang", "Természetes hang", "Gépiesebb hang", "Magyar hang kiválasztása", "Hang tesztelése")
        AlertDialog.Builder(this)
            .setTitle("JARVIS hangprofil")
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> setVoice(0.70f, 0.88f)
                    1 -> setVoice(1.0f, 1.0f)
                    2 -> setVoice(0.80f, 0.82f)
                    3 -> chooseHungarianVoice()
                    else -> reply("Üdvözöllek. JARVIS vagyok. Minden rendszer működésre kész.")
                }
            }.show()
    }

    private fun chooseHungarianVoice() {
        val voices = tts?.voices?.filter {
            it.locale.language == "hu" && !it.isNetworkConnectionRequired
        }?.sortedWith(compareBy({ it.quality }, { it.name }))?.reversed().orEmpty()
        if (voices.isEmpty()) {
            reply("A beszédmotor nem kínál telepített magyar hangot. Ellenőrizd a telefon szövegfelolvasási beállításait.")
            return
        }
        val names = voices.map { voice ->
            voice.name + if (voice.quality >= android.speech.tts.Voice.QUALITY_HIGH) " ★" else ""
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Magyar beszédhang kiválasztása")
            .setItems(names) { _, which ->
                val voice = voices[which]
                val applied = tts?.setVoice(voice) == TextToSpeech.SUCCESS
                if (applied) {
                    getSharedPreferences("jarvis_voice", MODE_PRIVATE).edit()
                        .putString("voiceName", voice.name).apply()
                    reply("A magyar hang kiválasztva. JARVIS jelentkezik.")
                } else reply("Ezt a hangot nem sikerült aktiválni.")
            }.show()
    }

    private fun setVoice(pitch: Float, rate: Float) {
        voicePitch = pitch
        voiceRate = rate
        getSharedPreferences("jarvis_voice", MODE_PRIVATE).edit()
            .putFloat("pitch", pitch).putFloat("rate", rate).apply()
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
        reply("Hangprofil beállítva. JARVIS készen áll.")
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            return
        }
        try {
            status.text = "● HALLGATLAK..."
            hud.mode = "listening"
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
        } else if (requestCode == speechRequest) {
            hud.mode = "idle"
            autoListen = false
            status.text = "● RENDSZER AKTÍV"
        }
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
            lower == "beszélgetési előzmények" || lower == "beszélgetés előzményei" ->
                reply(memory.summary())
            lower == "töröld a beszélgetést" || lower == "felejtsd el a beszélgetést" -> {
                memory.clear()
                reply("A mentett AI-beszélgetést töröltem.")
            }
            lower.startsWith("nyisd meg ") -> openApp(lower.removePrefix("nyisd meg ").trim())
            lower.startsWith("tárcsázd:") -> dial(command.substringAfter(":").trim())
            lower == "emlékeztess" || lower.startsWith("emlékeztető") -> openReminder()
            lower.contains("mit jegyeztél meg") || lower.contains("jegyzeteim") ->
                reply(prefs.getString("notes", "")?.takeIf { it.isNotBlank() } ?: "Még nincs jegyzeted.")
            lower.contains("idő") || lower.contains("óra") ->
                reply("Most " + SimpleDateFormat("HH:mm", hu).format(Date()) + " van.")
            lower.contains("dátum") || lower.contains("mai nap") ->
                reply("Ma " + SimpleDateFormat("yyyy. MMMM d., EEEE", hu).format(Date()) + " van.")
            lower.contains("szia") || lower.contains("helló") ->
                reply("Szia! JARVIS készen áll.")
            else -> askAi(command)
        }
    }

    private fun showAiSettings() {
        val field = EditText(this).apply {
            hint = "Gemini API-kulcs (AI Studio)"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("JARVIS online AI")
            .setMessage("A kulcsot a telefon titkosítva tárolja. Ne add meg másnak. A Gemini szolgáltatása adatokat kap a kérdéseidből; a használat kvótás vagy fizetős lehet. A kulcsot a Google AI Studio felületén hozhatod létre.")
            .setView(field)
            .setPositiveButton("Mentés") { _, _ ->
                try {
                    val key = field.text.toString().trim()
                    if (key.isBlank()) reply("Nem adtál meg kulcsot. A meglévő beállítás változatlan.")
                    else {
                        ai.saveKey(key)
                        reply("AI-kulcs titkosítva elmentve. Most már kérdezhetsz.")
                    }
                } catch (_: Exception) {
                    reply("Nem sikerült biztonságosan tárolni a kulcsot.")
                }
            }
            .setNeutralButton("Kulcs törlése") { _, _ ->
                ai.saveKey("")
                reply("Az AI-kulcsot töröltem.")
            }
            .setNegativeButton("Mégse", null)
            .create()
        dialog.show()
    }

    private fun askAi(question: String) {
        if (!ai.hasKey()) {
            reply("Az online AI használatához először add meg a saját Gemini API-kulcsodat az AI beállításainál. Az offline parancsok addig is működnek.")
            return
        }
        if (waitingForAi) {
            status.text = "● AI VÁLASZRA VÁR"
            return
        }
        waitingForAi = true
        output.text = "JARVIS gondolkodik..."
        hud.mode = "thinking"
        status.text = "● ONLINE AI KÉRÉS"
        executor.execute {
            var success = false
            val answer = try {
                val result = ai.ask(question, memory.messages())
                success = true
                result
            } catch (e: Exception) {
                "Az online AI most nem elérhető. Ellenőrizd az internetet, a kulcsot és a kvótát. " +
                    (e.message ?: "").take(160)
            }
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    waitingForAi = false
                    if (success) {
                        memory.add("user", question)
                        memory.add("model", answer)
                    }
                    reply(answer)
                }
            }
        }
    }

    private fun openApp(name: String) {
        val intent = when {
            name.contains("beállítás") -> Intent(Settings.ACTION_SETTINGS)
            name.contains("kamera") -> Intent("android.media.action.IMAGE_CAPTURE")
            name.contains("böngésző") || name.contains("internet") ->
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            name.contains("térkép") -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
            else -> null
        }
        if (intent == null) {
            reply("Ezt az alkalmazást még nem tudom megnyitni. Próbáld: beállítások, kamera, böngésző vagy térkép.")
            return
        }
        try {
            startActivity(intent)
            reply("Megnyitom a kért alkalmazást.")
        } catch (_: Exception) {
            reply("Nem található a megfelelő alkalmazás.")
        }
    }

    private fun dial(number: String) {
        if (!number.matches(Regex("[+0-9 ()-]{3,25}"))) {
            reply("Adj meg érvényes telefonszámot. Példa: tárcsázd: +3612345678")
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
            reply("Megnyitottam a tárcsázót. A hívást neked kell elindítanod.")
        } catch (_: Exception) {
            reply("Nem sikerült megnyitni a tárcsázót.")
        }
    }

    private fun openReminder() {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "JARVIS emlékeztető")
            }
            startActivity(intent)
            reply("Megnyitottam a naptárat. Állítsd be az időpontot és mentsd az eseményt.")
        } catch (_: Exception) {
            reply("Nem található naptáralkalmazás az emlékeztető létrehozásához.")
        }
    }

    private fun reply(message: String) {
        output.text = message
        status.text = "● RENDSZER AKTÍV"
        hud.mode = "speaking"
        tts?.speak(message.take(2500), TextToSpeech.QUEUE_FLUSH, null, "jarvis_response")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = hu
            tts?.setPitch(voicePitch)
            tts?.setSpeechRate(voiceRate)
            val savedVoice = getSharedPreferences("jarvis_voice", MODE_PRIVATE)
                .getString("voiceName", null)
            if (savedVoice != null) {
                tts?.voices?.firstOrNull { it.name == savedVoice && it.locale.language == "hu" }
                    ?.let { tts?.voice = it }
            } else {
                tts?.voices?.filter { it.locale.language == "hu" && !it.isNetworkConnectionRequired }
                    ?.maxByOrNull { it.quality }?.let { tts?.voice = it }
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            hud.mode = "idle"
                            if (autoListen) listen()
                        }
                    }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread { if (!isFinishing && !isDestroyed) hud.mode = "idle" }
                }
            })
        }
    }

    override fun onDestroy() {
        autoListen = false
        executor.shutdownNow()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
