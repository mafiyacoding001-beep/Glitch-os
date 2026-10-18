package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

data class VoiceOption(
    val id: String,
    val name: String,
    val language: String,
    val gender: String,
    val description: String
)

object EdgeNeuralTtsService {
    private const val TAG = "EdgeNeuralTts"
    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EA6542D086F3762310D348E1"
    private const val WSS_URL = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=$TRUSTED_CLIENT_TOKEN"

    val AVAILABLE_VOICES = listOf(
        VoiceOption("hi-IN-MadhurNeural", "Madhur (Hindi)", "Hindi / Hinglish", "Male", "Deep, energetic, viral shorts narrator"),
        VoiceOption("hi-IN-SwaraNeural", "Swara (Hindi)", "Hindi / Hinglish", "Female", "Expressive, crisp, engaging shorts host"),
        VoiceOption("en-IN-PrabhatNeural", "Prabhat (Indian English)", "English (IN)", "Male", "Fast, punchy Indian English narrator"),
        VoiceOption("en-IN-NeerjaNeural", "Neerja (Indian English)", "English (IN)", "Female", "Confident, modern Indian English host"),
        VoiceOption("en-US-GuyNeural", "Guy (US English)", "English (US)", "Male", "Deep, fast-paced dramatic narrator"),
        VoiceOption("en-US-JennyNeural", "Jenny (US English)", "English (US)", "Female", "Lively, high-energy viral podcast voice")
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private var androidTts: TextToSpeech? = null
    private var isAndroidTtsReady = false

    fun initAndroidTtsFallback(context: Context) {
        if (androidTts == null) {
            Handler(Looper.getMainLooper()).post {
                try {
                    androidTts = TextToSpeech(context.applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            isAndroidTtsReady = true
                            androidTts?.language = Locale("hi", "IN")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Android TTS init failed", e)
                }
            }
        }
    }

    private fun configureAndroidTtsVoice(tts: TextToSpeech, voiceName: String, rateMultiplier: Float = 1.25f) {
        val isMale = voiceName.contains("Madhur", ignoreCase = true) ||
                     voiceName.contains("Prabhat", ignoreCase = true) ||
                     voiceName.contains("Guy", ignoreCase = true)

        val targetLocale = if (voiceName.startsWith("hi")) Locale("hi", "IN") else Locale.US
        tts.language = targetLocale
        tts.setSpeechRate(rateMultiplier)
        tts.setPitch(if (isMale) 0.95f else 1.05f)

        try {
            val availableVoices = tts.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matched = availableVoices.firstOrNull { v ->
                    val nameLower = v.name.lowercase()
                    v.locale.language == targetLocale.language && (
                        if (isMale) (nameLower.contains("male") && !nameLower.contains("female")) || nameLower.contains("-male-")
                        else nameLower.contains("female") || nameLower.contains("-female-")
                    )
                } ?: availableVoices.firstOrNull { it.locale.language == targetLocale.language }

                if (matched != null) {
                    tts.voice = matched
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice matching exception: ${e.message}")
        }
    }

    /**
     * Synthesizes text to an MP3 file using Microsoft Edge Neural TTS -> Google TTS HTTP -> Android Native TTS.
     */
    suspend fun synthesizeSpeech(
        context: Context,
        text: String,
        voiceName: String = "hi-IN-MadhurNeural",
        rate: String = "+25%",
        pitch: String = "+0Hz",
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Text cannot be empty"))
        }

        initAndroidTtsFallback(context)

        // Tier 1: Microsoft Edge Neural WebSocket
        try {
            val edgeResult = trySynthesizeEdgeNeural(cleanText, voiceName, rate, pitch, outputFile)
            if (edgeResult.isSuccess && outputFile.exists() && outputFile.length() > 500) {
                return@withContext Result.success(outputFile)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Edge Neural TTS error: ${e.message}")
        }

        // Tier 2: Direct High-Speed Web Audio TTS Engine (StreamElements / Google TTS)
        try {
            val webResult = trySynthesizeWebHttpTts(cleanText, voiceName, outputFile)
            if (webResult.isSuccess && outputFile.exists() && outputFile.length() > 500) {
                return@withContext Result.success(outputFile)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Web HTTP TTS error: ${e.message}")
        }

        val globalFallbackEnabled =
            com.example.data.local.SettingsManager(context).settingsFlow.value.globalFallbackEnabled

        if (!globalFallbackEnabled) {
            Log.w(TAG, "Primary Cloud TTS failed and Global Fallback is DISABLED. Aborting fallback to Android Native TTS.")
            return@withContext Result.failure(Exception("Neural TTS generation failed and Global Fallback is disabled in Settings."))
        }

        // Tier 3: Android Native TextToSpeech Engine with strict Gender & Speed alignment
        try {
            Log.w(TAG, "Falling back to Android Native TTS")
            val nativeResult = trySynthesizeAndroidTts(context, cleanText, voiceName, outputFile)
            if (nativeResult.isSuccess && outputFile.exists() && outputFile.length() > 500) {
                return@withContext nativeResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Android Native TTS error: ${e.message}")
        }

        Result.failure(IllegalStateException("Speech synthesis providers failed; no narration audio was generated."))
    }

    private suspend fun trySynthesizeEdgeNeural(
        text: String,
        voiceName: String,
        rate: String,
        pitch: String,
        outputFile: File
    ): Result<File> {
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val url = "$WSS_URL&ConnectionId=$connectionId"

        val request = Request.Builder()
            .url(url)
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0")
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val deferred = CompletableDeferred<ByteArray>()
        val audioStream = ByteArrayOutputStream()

        val escapedText = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val ssmlLang = if (voiceName.startsWith("hi")) "hi-IN" else if (voiceName.startsWith("en-IN")) "en-IN" else "en-US"
        val ssmlPayload = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$ssmlLang'><voice name='$voiceName'><prosody pitch='$pitch' rate='$rate' volume='+0%'>$escapedText</prosody></voice></speak>"

        val dateFormat = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val timestamp = dateFormat.format(Date())

        val wsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val configMessage = "Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}"
                webSocket.send(configMessage)

                val ssmlMessage = "X-RequestId:$requestId\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:$timestamp\r\nPath:ssml\r\n\r\n$ssmlPayload"
                webSocket.send(ssmlMessage)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    webSocket.close(1000, "Completed")
                    deferred.complete(audioStream.toByteArray())
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val byteArray = bytes.toByteArray()
                if (byteArray.size > 2) {
                    val headerLength = ((byteArray[0].toInt() and 0xFF) shl 8) or (byteArray[1].toInt() and 0xFF)
                    if (byteArray.size > headerLength + 2) {
                        val headerStr = String(byteArray, 2, headerLength)
                        if (headerStr.contains("Path:audio")) {
                            val audioDataOffset = headerLength + 2
                            val audioDataLength = byteArray.size - audioDataOffset
                            audioStream.write(byteArray, audioDataOffset, audioDataLength)
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!deferred.isCompleted) {
                    if (audioStream.size() > 500) {
                        deferred.complete(audioStream.toByteArray())
                    } else {
                        deferred.completeExceptionally(t)
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!deferred.isCompleted) {
                    deferred.complete(audioStream.toByteArray())
                }
            }
        }

        val webSocket = client.newWebSocket(request, wsListener)

        return try {
            val audioBytes = withTimeoutOrNull(15000) {
                deferred.await()
            } ?: run {
                webSocket.cancel()
                throw Exception("Edge TTS response timeout")
            }

            if (audioBytes.isNotEmpty() && audioBytes.size > 500) {
                if (outputFile.parentFile?.exists() == false) {
                    outputFile.parentFile?.mkdirs()
                }
                FileOutputStream(outputFile).use { it.write(audioBytes) }
                Result.success(outputFile)
            } else {
                throw Exception("Received empty audio stream from Edge TTS")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun splitTextIntoChunks(text: String, maxChunkSize: Int = 180): List<String> {
        val sentences = text.split("(?<=[.!?।])\\s+".toRegex()).filter { it.isNotBlank() }
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        
        for (sentence in sentences) {
            if (currentChunk.length + sentence.length + 1 <= maxChunkSize) {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(" ")
                }
                currentChunk.append(sentence)
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
                if (sentence.length > maxChunkSize) {
                    val words = sentence.split(" ")
                    var temp = StringBuilder()
                    for (word in words) {
                        if (temp.length + word.length + 1 <= maxChunkSize) {
                            if (temp.isNotEmpty()) temp.append(" ")
                            temp.append(word)
                        } else {
                            if (temp.isNotEmpty()) {
                                chunks.add(temp.toString())
                                temp = StringBuilder()
                            }
                            temp.append(word)
                        }
                    }
                    if (temp.isNotEmpty()) {
                        currentChunk = temp
                    }
                } else {
                    currentChunk.append(sentence)
                }
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        return chunks
    }

    /**
     * Tier 2 Web HTTP TTS fallback (Google TTS / Voice Engine)
     */
    private suspend fun trySynthesizeWebHttpTts(
        text: String,
        voiceName: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val langCode = if (voiceName.startsWith("hi")) "hi" else if (voiceName.startsWith("en-IN")) "en-IN" else "en"
            val chunks = splitTextIntoChunks(text, 180)
            val chunkFiles = mutableListOf<File>()

            for (idx in chunks.indices) {
                val chunkText = chunks[idx]
                val encodedText = URLEncoder.encode(chunkText, "UTF-8")
                val urlString = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=$langCode&q=$encodedText"

                val tempFile = File(outputFile.parentFile, "chunk_${idx}_${System.currentTimeMillis()}.mp3")
                val url = URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.connect()

                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 500) {
                        chunkFiles.add(tempFile)
                    }
                } else {
                    Log.w(TAG, "Chunk $idx download failed with code: ${conn.responseCode}")
                }
            }

            if (chunkFiles.isNotEmpty()) {
                val mergeResult = mergeAudioFiles(chunkFiles, outputFile)
                // Cleanup temp files
                chunkFiles.forEach { try { it.delete() } catch (_: Exception) {} }
                return@withContext mergeResult
            }
            Result.failure(Exception("HTTP TTS failed to generate any audio chunks"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun trySynthesizeAndroidTts(
        context: Context,
        text: String,
        voiceName: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        initAndroidTtsFallback(context)
        val deferred = CompletableDeferred<File>()
        val utteranceId = "tts_${System.currentTimeMillis()}"

        try {
            // Wait up to 2 seconds for TTS to be ready if needed
            var attempts = 0
            while (!isAndroidTtsReady && attempts < 20) {
                kotlinx.coroutines.delay(100)
                attempts++
            }

            val tts = androidTts ?: run {
                return@withContext Result.failure(Exception("Android TTS Engine unavailable"))
            }

            configureAndroidTtsVoice(tts, voiceName, 1.25f)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}

                override fun onDone(id: String?) {
                    if (id == utteranceId && outputFile.exists() && outputFile.length() > 0) {
                        deferred.complete(outputFile)
                    } else {
                        deferred.completeExceptionally(Exception("Android TTS file creation failed"))
                    }
                }

                override fun onError(id: String?) {
                    deferred.completeExceptionally(Exception("Android TTS playback error"))
                }
            })

            val params = Bundle()
            val result = tts.synthesizeToFile(text, params, outputFile, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                return@withContext Result.failure(Exception("Failed to queue Android TTS synthesis"))
            }

            val file = withTimeoutOrNull(10000) { deferred.await() }
            if (file != null && file.exists() && file.length() > 0) {
                Result.success(file)
            } else {
                Result.failure(Exception("Android TTS timeout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct live speech backup if audio playback of file encounters issues
     */
    fun speakLive(context: Context, text: String, voiceName: String = "hi-IN-MadhurNeural", rateMultiplier: Float = 1.25f) {
        try {
            initAndroidTtsFallback(context)
            val tts = androidTts
            if (tts != null) {
                configureAndroidTtsVoice(tts, voiceName, rateMultiplier)
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "live_speech_${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Live speech error", e)
        }
    }

    /**
     * Synthesizes all storyboard scenes into one single continuous, high-definition Master Voiceover MP3.
     */
    suspend fun synthesizeMasterVoiceover(
        context: Context,
        scenes: List<Pair<String, String>>,
        voiceName: String = "hi-IN-MadhurNeural",
        rate: String = "+25%",
        pitch: String = "+0Hz",
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val combinedScript = scenes.joinToString(" ... ") { it.second.trim() }
        if (combinedScript.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Script is empty"))
        }

        // 1. Synthesize as a unified SSML speech stream
        val directSynth = synthesizeSpeech(
            context = context,
            text = combinedScript,
            voiceName = voiceName,
            rate = rate,
            pitch = pitch,
            outputFile = outputFile
        )

        if (directSynth.isSuccess && outputFile.exists() && outputFile.length() > 1000) {
            return@withContext directSynth
        }

        // 2. Fallback to file-based concatenation if individual files exist
        val individualFiles = scenes.mapIndexedNotNull { idx, (_, script) ->
            val tempFile = File(outputFile.parentFile, "temp_scene_${idx}_${System.currentTimeMillis()}.mp3")
            val res = kotlinx.coroutines.runBlocking {
                synthesizeSpeech(context, script, voiceName, rate, pitch, tempFile)
            }
            res.getOrNull()
        }

        if (individualFiles.isNotEmpty()) {
            return@withContext mergeAudioFiles(individualFiles, outputFile)
        }

        Result.failure(Exception("Master voiceover synthesis failed"))
    }

    /**
     * Merges multiple MP3 audio files sequentially into one complete Master Audio file.
     */
    fun mergeAudioFiles(audioFiles: List<File>, masterOutputFile: File): Result<File> {
        return try {
            if (masterOutputFile.parentFile?.exists() == false) {
                masterOutputFile.parentFile?.mkdirs()
            }
            FileOutputStream(masterOutputFile).use { output ->
                for (file in audioFiles) {
                    if (file.exists() && file.length() > 0) {
                        FileInputStream(file).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            if (masterOutputFile.exists() && masterOutputFile.length() > 0) {
                Result.success(masterOutputFile)
            } else {
                Result.failure(Exception("Merged audio file is empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
