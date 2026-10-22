package ru.example.voiceagent

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var speakButton: Button
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
        }
        val title = TextView(this).apply {
            text = "Голосовой агент"
            textSize = 28f
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "1. Разрешите микрофон.\\n2. Включите службу управления экраном в настройках специальных возможностей.\\n3. Нажмите кнопку и произнесите команду."
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        speakButton = Button(this).apply {
            text = "🎙 Сказать команду"
            setOnClickListener { listen() }
        }
        val settings = Button(this).apply {
            text = "Включить управление экраном"
            setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(title, LinearLayout.LayoutParams(-1, -2))
        layout.addView(status, LinearLayout.LayoutParams(-1, -2))
        layout.addView(speakButton, LinearLayout.LayoutParams(-1, -2))
        layout.addView(settings, LinearLayout.LayoutParams(-1, -2))
        setContentView(layout)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "На устройстве недоступно распознавание речи."
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "Слушаю… произнесите команду." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "Обрабатываю команду…" }
            override fun onError(error: Int) { status.text = "Не удалось распознать речь. Попробуйте ещё раз." }
            override fun onResults(results: Bundle?) {
                val command = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (command.isNullOrBlank()) {
                    status.text = "Команда не распознана."
                } else {
                    status.text = "Команда: «$command»"
                    sendBroadcast(Intent(PhoneControlService.ACTION_COMMAND).setPackage(packageName).putExtra("command", command))
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите команду")
        }
        recognizer?.startListening(intent)
    }

    override fun onInit(statusCode: Int) {
        tts?.language = Locale("ru", "RU")
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
