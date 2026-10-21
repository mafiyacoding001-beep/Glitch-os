package com.example.pipeline

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

interface VoiceGenerator {
    suspend fun generate(
        text: String,
        language: String,
        output: File,
        voiceName: String = "hi-IN-MadhurNeural",
        targetDurationSec: Int = 60
    )
}

/**
 * Multi-stage voiceover generator using cloud providers and Android TextToSpeech.
 */
class AndroidVoiceGenerator(
    private val context: Context
) : VoiceGenerator {

    override suspend fun generate(
        text: String,
        language: String,
        output: File,
        voiceName: String,
        targetDurationSec: Int
    ) = withContext(Dispatchers.IO) {
        require(targetDurationSec in 15..300) {
            "Narration duration must be between 15 and 300 seconds."
        }
        val cleanText = prepareTextForSpeech(text)
        require(cleanText.isNotBlank()) {
            "Narration text is required before generating speech."
        }
        require(speechLanguageCode(language) != null) {
            "Unsupported narration language: $language."
        }
        val targetSec = targetDurationSec.toDouble()

        val parent = output.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw IOException("Could not create narration output directory: ${parent.absolutePath}")
        }
        if (output.exists() && !output.delete()) {
            throw IOException("Could not replace existing narration output: ${output.absolutePath}")
        }

        // 1. Try Cloud Voice APIs (StreamElements / Google TTS)
        val cloudSuccess = tryCloudTtsEngines(cleanText, language, voiceName, output, targetSec)
        if (cloudSuccess && output.exists() && output.length() > 2000) {
            return@withContext
        }

        // 2. Try Android Native System TextToSpeech (synthesizeToFile)
        val nativeSuccess = tryNativeSystemTts(cleanText, language, output)
        if (nativeSuccess && output.exists() && output.length() > 2000) {
            return@withContext
        }

        if (output.exists() && !output.delete()) {
            throw IOException("Could not remove incomplete narration output: ${output.absolutePath}")
        }
        throw IOException("Speech providers did not produce valid narration audio.")
    }

    private fun prepareTextForSpeech(text: String): String {
        return text
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tryCloudTtsEngines(
        text: String,
        language: String,
        voiceName: String,
        output: File,
        targetSec: Double
    ): Boolean {
        val langCode = speechLanguageCode(language) ?: return false
        val isHindi = langCode == "hi"
        val pollyVoice = mapToPollyVoice(voiceName, isHindi)
        val chunks = splitTextIntoChunks(text, maxLen = 180)
        if (chunks.isEmpty()) return false

        return try {
            val pcmBaos = ByteArrayOutputStream()
            var detectedSampleRate = 24000
            var detectedChannels = 1

            val tempDir = File(context.cacheDir, "tts_temp_${System.currentTimeMillis()}").apply { mkdirs() }

            try {
                for ((idx, chunk) in chunks.withIndex()) {
                    val bytes = fetchChunkAudio(chunk, pollyVoice, langCode) ?: return false
                    if (bytes.size <= 200) return false

                    val tempChunkFile = File(tempDir, "chunk_$idx.mp3")
                    tempChunkFile.writeBytes(bytes)

                    val decodedPcm = com.example.util.FreeMusicManager.decodeAudioToPcm(tempChunkFile)
                    if (decodedPcm != null && decodedPcm.pcmData.isNotEmpty()) {
                        detectedSampleRate = decodedPcm.sampleRate
                        detectedChannels = decodedPcm.channelCount
                        pcmBaos.write(decodedPcm.pcmData)
                    } else {
                        return false
                    }
                    try { tempChunkFile.delete() } catch (_: Exception) {}
                }
            } finally {
                try { tempDir.deleteRecursively() } catch (_: Exception) {}
            }

            val rawPcm = pcmBaos.toByteArray()
            if (rawPcm.size <= 500) return false

            val bytesPerSample = 2 * detectedChannels
            val totalSamples = rawPcm.size / bytesPerSample
            val actualSec = totalSamples.toDouble() / detectedSampleRate

            if (actualSec > 0.5 && actualSec < targetSec * 0.85) {
                val targetByteCount =
                    (targetSec * detectedSampleRate * detectedChannels * 2).toInt()
                val silenceByteCount = (targetByteCount - rawPcm.size).coerceAtLeast(0)
                pcmBaos.write(ByteArray(silenceByteCount))
            }

            val finalBytes = pcmBaos.toByteArray()
            val finalTotalSamples = finalBytes.size / bytesPerSample

            FileOutputStream(output).use { fos ->
                writeWavHeader(fos, finalTotalSamples, detectedChannels, detectedSampleRate, 16)
                fos.write(finalBytes)
            }
            output.exists() && output.length() > 2000
        } catch (e: Exception) {
            android.util.Log.e("VoiceGenerator", "tryCloudTtsEngines error: ${e.message}", e)
            if (output.exists()) output.delete()
            false
        }
    }

    private fun writeWavHeader(
        out: java.io.OutputStream,
        totalSamples: Int,
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val subChunk2Size = totalSamples * channels * bitsPerSample / 8
        val chunkSize = 36 + subChunk2Size

        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        bb.put('R'.code.toByte()); bb.put('I'.code.toByte()); bb.put('F'.code.toByte()); bb.put('F'.code.toByte())
        bb.putInt(chunkSize)
        bb.put('W'.code.toByte()); bb.put('A'.code.toByte()); bb.put('V'.code.toByte()); bb.put('E'.code.toByte())

        bb.put('f'.code.toByte()); bb.put('m'.code.toByte()); bb.put('t'.code.toByte()); bb.put(' '.code.toByte())
        bb.putInt(16)
        bb.putShort(1.toShort())
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort(blockAlign.toShort())
        bb.putShort(bitsPerSample.toShort())

        bb.put('d'.code.toByte()); bb.put('a'.code.toByte()); bb.put('t'.code.toByte()); bb.put('a'.code.toByte())
        bb.putInt(subChunk2Size)

        out.write(header)
    }

    private fun fetchChunkAudio(chunk: String, pollyVoice: String, langCode: String): ByteArray? {
        if (langCode == "hi" || langCode == "en") {
            val seBytes = fetchStreamElementsAudio(chunk, pollyVoice)
            if (seBytes != null && seBytes.size > 200) return seBytes
        }

        // Google Translate TTS Gateway
        val googleBytes = fetchGoogleTranslateAudio(chunk, langCode)
        if (googleBytes != null && googleBytes.size > 200) return googleBytes

        return null
    }

    private fun fetchStreamElementsAudio(chunk: String, voice: String): ByteArray? {
        return try {
            val encoded = URLEncoder.encode(chunk, "UTF-8")
            val urlStr = "https://api.streamelements.com/kappa/v2/speech?voice=$voice&text=$encoded"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size > 200) return bytes
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchGoogleTranslateAudio(chunk: String, langCode: String): ByteArray? {
        return try {
            val encoded = URLEncoder.encode(chunk, "UTF-8")
            val urlStr = "https://translate.google.com/translate_tts?ie=UTF-8&tl=$langCode&client=tw-ob&q=$encoded"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size > 200) return bytes
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun mapToPollyVoice(voicePreference: String, isHindi: Boolean): String {
        return when {
            isHindi -> "Aditi"
            voicePreference.contains("Female", ignoreCase = true) -> "Joanna"
            voicePreference.contains("Male", ignoreCase = true) -> "Joey"
            voicePreference.contains("British", ignoreCase = true) -> "Brian"
            else -> "Joey"
        }
    }

    private suspend fun tryNativeSystemTts(
        text: String,
        language: String,
        output: File
    ): Boolean = withContext(Dispatchers.Main) {
        val locale = speechLanguageCode(language)?.let { Locale.forLanguageTag(it) }
            ?: return@withContext false
        val latch = CountDownLatch(1)
        var success = false
        var tts: TextToSpeech? = null
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: run {
                        latch.countDown()
                        return@TextToSpeech
                    }
                    if (engine.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) {
                        latch.countDown()
                        return@TextToSpeech
                    }
                    engine.language = locale
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            success = true
                            latch.countDown()
                        }
                        override fun onError(utteranceId: String?) {
                            latch.countDown()
                        }
                    })
                    val params = Bundle()
                    val utteranceId = UUID.randomUUID().toString()
                    val res = tts?.synthesizeToFile(text, params, output, utteranceId)
                    if (res != TextToSpeech.SUCCESS) {
                        latch.countDown()
                    }
                } else {
                    latch.countDown()
                }
            }
            withContext(Dispatchers.IO) {
                latch.await(10, TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
            android.util.Log.w("VoiceGenerator", "Native TextToSpeech failed: ${e.message}", e)
        } finally {
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (_: Exception) {}
        }
        success && output.exists() && output.length() > 1000
    }

    private fun speechLanguageCode(language: String): String? {
        val normalized = language.trim().lowercase(Locale.ROOT)
        return when {
            normalized == "hi" || normalized.startsWith("hindi") -> "hi"
            normalized == "en" || normalized.startsWith("english") -> "en"
            normalized == "es" || normalized.startsWith("spanish") -> "es"
            normalized == "fr" || normalized.startsWith("french") -> "fr"
            normalized == "de" || normalized.startsWith("german") -> "de"
            else -> null
        }
    }

    private fun splitTextIntoChunks(text: String, maxLen: Int): List<String> {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return emptyList()
        if (clean.length <= maxLen) return listOf(clean)

        val parts = clean.split(Regex("(?<=[.!?,|\\n ])\\s+"))
        val list = mutableListOf<String>()
        var sb = StringBuilder()
        for (p in parts) {
            if (sb.length + p.length + 1 <= maxLen) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(p)
            } else {
                if (sb.isNotEmpty()) {
                    list.add(sb.toString())
                    sb = StringBuilder()
                }
                sb.append(p)
            }
        }
        if (sb.isNotEmpty()) list.add(sb.toString())
        return list
    }
}
