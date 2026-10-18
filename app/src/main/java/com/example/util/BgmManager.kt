package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sin

/**
 * Royalty-Free Background Music (BGM) Manager & Procedural Audio Engine.
 * Provides mood-based cinematic BGM without requiring any external API key.
 */
object BgmManager {
    private const val TAG = "BgmManager"

    data class BgmTrackInfo(
        val moodName: String,
        val cdnUrl: String,
        val description: String
    )

    val MOOD_TRACKS = mapOf(
        "Dark / Mystery" to BgmTrackInfo(
            "Dark / Mystery",
            "https://ia800301.us.archive.org/11/items/CinematicAmbientLoop/Cinematic_Dark_Suspense_Pad.mp3",
            "Deep suspenseful drone pad for dark history & mystery shorts"
        ),
        "Cyberpunk / Tech" to BgmTrackInfo(
            "Cyberpunk / Tech",
            "https://ia800201.us.archive.org/3/items/CyberpunkSynthLoop/Cyberpunk_Dark_Synth_Loop.mp3",
            "Futuristic dark synthwave loop for AI & technology topics"
        ),
        "Cinematic / Dramatic" to BgmTrackInfo(
            "Cinematic / Dramatic",
            "https://ia800300.us.archive.org/5/items/CinematicPads/Cinematic_Orchestral_Dramatic_Pad.mp3",
            "Orchestral emotional pad for storytelling & documentaries"
        ),
        "Energetic / Viral" to BgmTrackInfo(
            "Energetic / Viral",
            "https://ia800400.us.archive.org/2/items/LofiShortsBgm/Lofi_Viral_Rhythm_Loop.mp3",
            "Upbeat lo-fi rhythm loop for high retention shorts"
        )
    )

    /**
     * Downloads or generates BGM for the selected niche/mood.
     */
    suspend fun prepareBgmTrack(context: Context, nicheOrMood: String, durationSec: Int): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "bgm_tracks").apply { if (!exists()) mkdirs() }
        val matchedMood = MOOD_TRACKS.keys.find { nicheOrMood.contains(it, ignoreCase = true) } ?: "Dark / Mystery"
        val trackInfo = MOOD_TRACKS[matchedMood] ?: MOOD_TRACKS.values.first()
        val targetFile = File(cacheDir, "bgm_${matchedMood.replace("[^a-zA-Z]".toRegex(), "")}.mp3")

        if (targetFile.exists() && targetFile.length() > 50_000L) {
            return@withContext targetFile
        }

        // 1. Try downloading CDN royalty-free track
        try {
            val url = URL(trackInfo.cdnUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (targetFile.length() > 10_000L) {
                    return@withContext targetFile
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network download of BGM failed, falling back to procedural ambient generator: ${e.message}")
        }

        val globalFallbackEnabled =
            com.example.data.local.SettingsManager(context).settingsFlow.value.globalFallbackEnabled

        if (!globalFallbackEnabled) {
            Log.w(TAG, "Network download of BGM failed and Global Fallback is DISABLED. Strict API mode active.")
            throw IllegalStateException("BGM audio download failed and Global Fallback is disabled in Settings.")
        }

        // 2. Procedural Ambient Drone Generator (100% Offline Fallback - No API Key Needed)
        return@withContext generateProceduralAmbientWav(context, durationSec, matchedMood)
    }

    /**
     * Generates a 100% royalty-free cinematic ambient audio track procedurally using PCM synthesis.
     */
    private fun generateProceduralAmbientWav(context: Context, durationSec: Int, mood: String): File {
        val cacheDir = File(context.cacheDir, "bgm_tracks").apply { if (!exists()) mkdirs() }
        val targetFile = File(cacheDir, "procedural_bgm_${durationSec}s.wav")

        val sampleRate = 44100
        val totalSamples = durationSec * sampleRate
        val pcmData = ByteArray(totalSamples * 2) // 16-bit mono

        val baseFreq = when {
            mood.contains("Dark", ignoreCase = true) -> 55.0 // A1 deep drone
            mood.contains("Cyber", ignoreCase = true) -> 65.41 // C2 synth drone
            mood.contains("Cinematic", ignoreCase = true) -> 73.42 // D2 cinematic pad
            else -> 82.41 // E2 warm lo-fi
        }

        var pcmIndex = 0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Layer 3 harmonic sine waves for a rich cinematic drone pad
            val wave1 = sin(2.0 * Math.PI * baseFreq * t)
            val wave2 = sin(2.0 * Math.PI * (baseFreq * 1.5) * t) * 0.4 // Fifth harmonic
            val wave3 = sin(2.0 * Math.PI * (baseFreq * 2.01) * t) * 0.2 // Octave shimmer
            val lfo = 0.6 + 0.4 * sin(2.0 * Math.PI * 0.2 * t) // 0.2Hz slow modulation filter

            val combined = ((wave1 + wave2 + wave3) / 1.6) * lfo * 0.15 // Low 15% volume
            val sampleVal = (combined * 32767).toInt().coerceIn(-32768, 32767).toShort()

            pcmData[pcmIndex++] = (sampleVal.toInt() and 0xFF).toByte()
            pcmData[pcmIndex++] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()
        }

        writePcmToWavFile(pcmData, sampleRate, 1, targetFile)
        return targetFile
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
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // 16-bit PCM format
        header[20] = 1; header[21] = 0 // Audio format = 1 (PCM)
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte(); header[33] = 0 // Block align
        header[34] = 16; header[35] = 0 // Bits per sample
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

    /**
     * Blends Voiceover track (100% volume) and BGM track (15-20% low volume) into a master mixed audio file.
     */
    suspend fun blendVoiceoverWithBgm(
        context: Context,
        voiceoverFile: File,
        bgmFile: File,
        bgmVolumeRatio: Float = 0.15f
    ): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "mixed_audio").apply { if (!exists()) mkdirs() }
        val outputFile = File(cacheDir, "master_mixed_${System.currentTimeMillis()}.wav")

        val ffmpegBin = File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        if (ffmpegBin.exists() && ffmpegBin.canExecute()) {
            try {
                val cmd = listOf(
                    ffmpegBin.absolutePath, "-y",
                    "-i", voiceoverFile.absolutePath,
                    "-i", bgmFile.absolutePath,
                    "-filter_complex", "[0:a]volume=1.0[v];[1:a]volume=$bgmVolumeRatio,aloop=loop=-1:size=2e+09[bg];[v][bg]amix=inputs=2:duration=first:dropout_transition=2",
                    "-c:a", "aac", "-b:a", "192k",
                    outputFile.absolutePath
                )
                val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
                val exit = proc.waitFor()
                if (exit == 0 && outputFile.exists() && outputFile.length() > 5000L) {
                    return@withContext outputFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "FFmpeg audio mix failed, falling back to pure voiceover: ${e.message}")
            }
        }

        voiceoverFile
    }
}
