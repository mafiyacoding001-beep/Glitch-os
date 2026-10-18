package com.example.ui.keyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Transparent helper activity to request RECORD_AUDIO permission and/or
 * invoke speech recognition on behalf of the InputMethodService.
 */
class VoiceInputHelperActivity : ComponentActivity() {

    companion object {
        var onResultCallback: ((String) -> Unit)? = null
        var onErrorCallback: ((String) -> Unit)? = null
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull()
            if (!recognizedText.isNullOrBlank()) {
                onResultCallback?.invoke(recognizedText)
            } else {
                onErrorCallback?.invoke("No speech detected")
            }
        } else {
            onErrorCallback?.invoke("Voice recognition cancelled")
        }
        finish()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechRecognitionIntent()
        } else {
            onErrorCallback?.invoke("Microphone permission denied. Please allow in Settings.")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startSpeechRecognitionIntent()
        }
    }

    private fun startSpeechRecognitionIntent() {
        val lang = intent.getStringExtra("EXTRA_LANG") ?: "en-US"
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now / बोलिए...")
        }
        try {
            speechLauncher.launch(speechIntent)
        } catch (e: Exception) {
            onErrorCallback?.invoke("Speech recognition unavailable: ${e.message}")
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not clear callbacks here if finish was called
    }
}
