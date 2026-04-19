package com.smarttour360.app.ui.chatbot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onStateChange: (VoiceState) -> Unit
) {
    enum class VoiceState {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentState = VoiceState.IDLE

    init {
        initSpeechRecognizer()
        initTts()
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                    setState(VoiceState.PROCESSING)
                    onSpeechResult(text)
                }

                override fun onBeginningOfSpeech() = setState(VoiceState.LISTENING)
                override fun onError(error: Int) = setState(VoiceState.IDLE)
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("en", "IN")
                tts?.setSpeechRate(0.95f)
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = setState(VoiceState.SPEAKING)
                    override fun onDone(utteranceId: String?) = setState(VoiceState.IDLE)
                    override fun onError(utteranceId: String?) = setState(VoiceState.IDLE)
                })
            }
        }
    }

    fun startListening() {
        if (currentState == VoiceState.SPEAKING) {
            stopSpeaking()
            return
        }
        if (currentState != VoiceState.IDLE) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        setState(VoiceState.LISTENING)
        speechRecognizer?.startListening(intent)
    }

    fun speak(text: String) {
        if (!ttsReady) {
            setState(VoiceState.IDLE)
            return
        }
        val clean = text
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("#+ "), "")
            .trim()
        if (clean.isBlank()) {
            setState(VoiceState.IDLE)
            return
        }
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "smarttour_reply")
    }

    fun stopSpeaking() {
        tts?.stop()
        setState(VoiceState.IDLE)
    }

    fun finishProcessingWithoutSpeech() {
        if (currentState == VoiceState.PROCESSING) {
            setState(VoiceState.IDLE)
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.shutdown()
        speechRecognizer = null
        tts = null
    }

    private fun setState(state: VoiceState) {
        currentState = state
        onStateChange(state)
    }
}
