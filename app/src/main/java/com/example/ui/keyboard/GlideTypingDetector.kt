package com.example.ui.keyboard

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class GlidePoint(
    val offset: Offset,
    val timestamp: Long = System.currentTimeMillis()
)

object GlideTypingDecoder {

    /**
     * Decode touch trail points to find target candidate words based on key coordinates
     */
    fun decodeTrail(
        points: List<Offset>,
        keyPositions: Map<String, Offset>
    ): List<WordSuggestion> {
        if (points.size < 5 || keyPositions.isEmpty()) return emptyList()

        // Extract key sequence sampled from path curve peaks
        val sampledChars = mutableListOf<Char>()
        var lastOffset: Offset? = null

        for (pt in points) {
            val nearestKey = keyPositions.minByOrNull { hypot(it.value.x - pt.x, it.value.y - pt.y) }
            if (nearestKey != null) {
                val char = nearestKey.key.firstOrNull()?.lowercaseChar()
                if (char != null && (sampledChars.isEmpty() || sampledChars.last() != char)) {
                    val distFromLast = lastOffset?.let { hypot(it.x - pt.x, it.y - pt.y) } ?: 100f
                    if (distFromLast > 15f) {
                        sampledChars.add(char)
                        lastOffset = pt
                    }
                }
            }
        }

        if (sampledChars.size < 2) return emptyList()

        val sampleStr = sampledChars.joinToString("")
        val startChar = sampleStr.first()
        val endChar = sampleStr.last()

        // Query dictionary suggestions starting with startChar and ending near endChar
        val rawSuggestions = KeyboardAlgorithms.getSuggestions(startChar.toString(), maxResults = 10)

        val filtered = rawSuggestions
            .filter { sugg ->
                val word = sugg.word.lowercase()
                word.length >= 2 && word.endsWith(endChar)
            }
            .sortedBy { levenshteinDistance(sampleStr, it.word.lowercase()) }

        return if (filtered.isNotEmpty()) {
            filtered.take(3)
        } else {
            rawSuggestions.take(3)
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        return KeyboardAlgorithms.levenshteinDistance(s1, s2)
    }
}
