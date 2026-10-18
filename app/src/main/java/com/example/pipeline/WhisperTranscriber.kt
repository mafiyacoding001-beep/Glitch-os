package com.example.pipeline

import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface WhisperTranscriber {
    suspend fun transcribe(
        audio: File,
        outputSrt: File,
        language: String?,
        narrationText: String? = null,
        targetDurationSec: Double = 0.0
    )
}

class AndroidWhisperTranscriber : WhisperTranscriber {

    override suspend fun transcribe(
        audio: File,
        outputSrt: File,
        language: String?,
        narrationText: String?,
        targetDurationSec: Double
    ) = withContext(Dispatchers.IO) {
        outputSrt.parentFile?.mkdirs()
        val audioDur = getAudioDurationSec(audio)
        val durationSec = maxOf(audioDur, targetDurationSec).coerceAtLeast(15.0)

        val srtContent = if (!narrationText.isNullOrBlank()) {
            generateNarrationSrt(narrationText, durationSec)
        } else {
            generateWordLevelSrt(durationSec)
        }

        outputSrt.writeText(srtContent)
    }

    private fun getAudioDurationSec(file: File): Double {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            (durationStr?.toLongOrNull() ?: 30_000L) / 1000.0
        } catch (_: Exception) {
            30.0
        }
    }

    private fun generateNarrationSrt(narration: String, totalDurationSec: Double): String {
        val cleanText = narration
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanText.isEmpty()) {
            return generateWordLevelSrt(totalDurationSec)
        }

        val allWords = cleanText.split(" ").filter { it.isNotBlank() }
        if (allWords.isEmpty()) {
            return generateWordLevelSrt(totalDurationSec)
        }

        // Group words into clean, readable 3-5 word subtitle chunks
        // (Avoid 1-2 words rapid flashing so viewers can read comfortably)
        val cuesList = mutableListOf<String>()
        var tempIndex = 0
        while (tempIndex < allWords.size) {
            val chunkWords = mutableListOf<String>()
            var currentLen = 0
            while (tempIndex < allWords.size && chunkWords.size < 4 && currentLen + allWords[tempIndex].length < 28) {
                chunkWords.add(allWords[tempIndex])
                currentLen += allWords[tempIndex].length + 1
                tempIndex++
            }
            if (chunkWords.isEmpty() && tempIndex < allWords.size) {
                chunkWords.add(allWords[tempIndex])
                tempIndex++
            }
            if (chunkWords.isNotEmpty()) {
                cuesList.add(chunkWords.joinToString(" "))
            }
        }

        // Distribute timing across the exact audio duration without premature capping or rapid flashing
        val sb = StringBuilder()
        var currentStartSec = 0.0
        var cueIndexCounter = 1

        val estSpeechSec = allWords.size / 2.3
        val numLoops = if (estSpeechSec > 0.5 && estSpeechSec < totalDurationSec * 0.85) {
            Math.ceil(totalDurationSec / estSpeechSec).toInt().coerceIn(1, 10)
        } else {
            1
        }

        val fullCuesList = mutableListOf<String>()
        for (l in 0 until numLoops) {
            fullCuesList.addAll(cuesList)
        }

        val fullTotalChars = fullCuesList.sumOf { it.length }.coerceAtLeast(1)

        for (i in fullCuesList.indices) {
            val cueText = fullCuesList[i]
            val charRatio = cueText.length.toDouble() / fullTotalChars
            val calculatedDuration = totalDurationSec * charRatio
            val durationForCue = maxOf(calculatedDuration, 1.2)
            
            val endSec = if (i == fullCuesList.indices.last) {
                totalDurationSec
            } else {
                (currentStartSec + durationForCue).coerceAtMost(totalDurationSec - ((fullCuesList.size - 1 - i) * 0.5))
            }.coerceAtLeast(currentStartSec + 0.8)

            sb.append("${cueIndexCounter++}\n")
            sb.append("${formatSrtTimestamp(currentStartSec)} --> ${formatSrtTimestamp(endSec)}\n")
            sb.append("$cueText\n\n")

            currentStartSec = endSec
            if (currentStartSec >= totalDurationSec) break
        }

        return sb.toString()
    }

    private fun generateWordLevelSrt(totalDurationSec: Double): String {
        val sb = StringBuilder()
        val chunkDuration = 2.5
        val chunks = Math.ceil(totalDurationSec / chunkDuration).toInt().coerceAtLeast(1)

        for (i in 0 until chunks) {
            val start = i * chunkDuration
            val end = ((i + 1) * chunkDuration).coerceAtMost(totalDurationSec)

            sb.append("${i + 1}\n")
            sb.append("${formatSrtTimestamp(start)} --> ${formatSrtTimestamp(end)}\n")
            sb.append("Scene ${i + 1}\n\n")
        }

        return sb.toString()
    }

    private fun formatSrtTimestamp(seconds: Double): String {
        val totalMs = Math.round(seconds * 1000).toLong()
        val hours = totalMs / 3600000
        val remainder1 = totalMs % 3600000
        val minutes = remainder1 / 60000
        val remainder2 = remainder1 % 60000
        val secs = remainder2 / 1000
        val ms = remainder2 % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, ms)
    }
}
