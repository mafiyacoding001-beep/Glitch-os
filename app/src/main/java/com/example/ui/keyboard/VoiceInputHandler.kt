package com.example.ui.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

class VoiceInputHandler(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onPartialSpeech: ((String) -> Unit)? = null,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var isListening by mutableStateOf(false)
        private set

    var recognizedPartialText by mutableStateOf("")
        private set

    fun startListening(languageCode: String = "en-US") {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            launchHelperActivity(languageCode)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // Fallback to system speech dialog intent
            launchHelperActivity(languageCode)
            return
        }

        stopListening()

        mainHandler.post {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            recognizedPartialText = "Listening..."
                            onPartialSpeech?.invoke("🎙️ Listening...")
                        }

                        override fun onBeginningOfSpeech() {
                            onPartialSpeech?.invoke("🎙️ Speaking...")
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                            onPartialSpeech?.invoke("Processing voice...")
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission missing"
                                else -> "Speech error ($error)"
                            }

                            // If in-service recognition fails, try helper activity fallback
                            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                                error == SpeechRecognizer.ERROR_AUDIO ||
                                error == SpeechRecognizer.ERROR_CLIENT
                            ) {
                                launchHelperActivity(languageCode)
                            } else {
                                onError(message)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val bestResult = matches?.firstOrNull()
                            if (!bestResult.isNullOrBlank()) {
                                onSpeechResult(bestResult)
                            } else {
                                onError("No speech detected")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull()
                            if (!partial.isNullOrEmpty()) {
                                recognizedPartialText = partial
                                onPartialSpeech?.invoke("🎙️ $partial")
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                launchHelperActivity(languageCode)
            }
        }
    }

    private fun launchHelperActivity(languageCode: String) {
        VoiceInputHelperActivity.onResultCallback = { result ->
            onSpeechResult(result)
        }
        VoiceInputHelperActivity.onErrorCallback = { errorMsg ->
            onError(errorMsg)
        }

        try {
            val intent = Intent(context, VoiceInputHelperActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_LANG", languageCode)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            onError("Cannot start voice activity: ${e.message}")
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                // Ignore destruction error
            }
            speechRecognizer = null
            isListening = false
        }
    }
}
