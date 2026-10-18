package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

/**
 * Royalty-Free Swoosh & Pop SFX Manager.
 * Downloads from GitHub raw sound effect repositories or generates
 * high-quality PCM Swoosh & Pop transition sound effects offline.
 */
object SfxManager {
    private const val TAG = "SfxManager"

    private const val GITHUB_WHOOSH_URL = "https://raw.githubusercontent.com/rafaelreis-hotmart/sound-effects/main/whoosh.wav"
    private const val GITHUB_POP_URL = "https://raw.githubusercontent.com/rafaelreis-hotmart/sound-effects/main/pop.wav"

    /**
     * Prepares Swoosh SFX File (Network GitHub or Procedural fallback).
     */
    suspend fun getSwooshSfx(context: Context): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "sfx_cache").apply { if (!exists()) mkdirs() }
        val targetFile = File(cacheDir, "swoosh_transition.wav")

        if (targetFile.exists() && targetFile.length() > 2000L) {
            return@withContext targetFile
        }

        // 1. Try GitHub Raw SFX Download
        try {
            val url = URL(GITHUB_WHOOSH_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { out -> input.copyTo(out) }
                }
                if (targetFile.length() > 2000L) return@withContext targetFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Whoosh download skipped, using procedural PCM generator: ${e.message}")
        }

        // 2. Procedural Swoosh Generator
        return@withContext generateProceduralSwoosh(targetFile)
    }

    /**
     * Prepares Pop SFX File (Network GitHub or Procedural fallback).
     */
    suspend fun getPopSfx(context: Context): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "sfx_cache").apply { if (!exists()) mkdirs() }
        val targetFile = File(cacheDir, "pop_caption.wav")

        if (targetFile.exists() && targetFile.length() > 1000L) {
            return@withContext targetFile
        }

        // 1. Try GitHub Raw SFX Download
        try {
            val url = URL(GITHUB_POP_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { out -> input.copyTo(out) }
                }
                if (targetFile.length() > 1000L) return@withContext targetFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub Pop download skipped, using procedural PCM generator: ${e.message}")
        }

        // 2. Procedural Pop Generator
        return@withContext generateProceduralPop(targetFile)
    }

    /**
     * Generates a 220ms cinematic air-swoosh noise transition SFX.
     */
    private fun generateProceduralSwoosh(outputFile: File, durationMs: Int = 220): File {
        val sampleRate = 44100
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(totalSamples * 2)

        var pcmIdx = 0
        val random = Random()
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val envelope = Math.sin(progress * Math.PI) // Smooth bell curve volume
            val noise = (random.nextDouble() * 2.0 - 1.0)
            val sampleVal = (noise * envelope * 0.40 * 32767).toInt().coerceIn(-32768, 32767).toShort()

            pcmData[pcmIdx++] = (sampleVal.toInt() and 0xFF).toByte()
            pcmData[pcmIdx++] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        writePcmToWavFile(pcmData, sampleRate, 1, outputFile)
        return outputFile
    }

    /**
     * Generates an 80ms exponential pitch-drop UI pop SFX.
     */
    private fun generateProceduralPop(outputFile: File, durationMs: Int = 80): File {
        val sampleRate = 44100
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(totalSamples * 2)

        var phase = 0.0
        var pcmIdx = 0
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = 850.0 * Math.exp(-4.5 * progress) + 120.0
            val envelope = Math.exp(-5.0 * progress)

            phase += 2.0 * Math.PI * currentFreq / sampleRate
            val sampleVal = (Math.sin(phase) * envelope * 0.50 * 32767).toInt().coerceIn(-32768, 32767).toShort()

            pcmData[pcmIdx++] = (sampleVal.toInt() and 0xFF).toByte()
            pcmData[pcmIdx++] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }
        writePcmToWavFile(pcmData, sampleRate, 1, outputFile)
        return outputFile
    }

    private fun writePcmToWavFile(pcmData: ByteArray, sampleRate: Int, channels: Int, outputFile: File) {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * 2).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte(); header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        FileOutputStream(outputFile).use { out ->
            out.write(header)
            out.write(pcmData)
        }
    }
}
