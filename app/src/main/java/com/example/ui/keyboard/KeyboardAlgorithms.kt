package com.example.ui.keyboard

import kotlin.math.min

object KeyboardAlgorithms {

    private val commonDictionary = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "keyboard", "glitch", "android", "compose", "application", "developer", "quick",
        "smart", "suggestion", "kotlin", "code", "design", "algorithm", "speed", "typing",
        "message", "hello", "world", "thanks", "great", "awesome", "today", "tomorrow",
        "please", "friend", "happy", "custom", "system", "gesture", "emoji", "clipboard",
        "cursor", "spacebar", "backspace", "layout", "qwerty", "material", "dynamic",
        "phone", "email", "online", "number", "turbo", "batch", "sequence", "magic"
    )

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var isWord = false
        var frequency = 0
    }

    private val root = TrieNode()

    init {
        commonDictionary.forEachIndexed { index, word ->
            insertWord(word.lowercase().trim(), frequency = 1000 - index)
        }
    }

    fun insertWord(word: String, frequency: Int = 1) {
        val clean = word.trim().lowercase()
        if (clean.isEmpty() || clean.contains(" ")) return
        var current = root
        for (char in clean) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        current.isWord = true
        current.frequency += frequency
    }

    /**
     * Ultra-fast prefix search + light fuzzy correction.
     * Guaranteed strictly single word results.
     */
    fun getSuggestions(currentPrefix: String, maxResults: Int = 3): List<WordSuggestion> {
        val cleanPrefix = currentPrefix.trim().split(Regex("\\s+")).lastOrNull() ?: ""
        if (cleanPrefix.isBlank()) return emptyList()

        val lowerPrefix = cleanPrefix.lowercase()
        val results = mutableListOf<WordSuggestion>()

        // 1. Exact Trie prefix matches (O(L) - instantaneous)
        val trieMatches = findTriePrefixMatches(lowerPrefix)
        results.addAll(trieMatches)

        // 2. Light fuzzy check only if trie matches are low and word is appropriate length
        if (results.size < maxResults && lowerPrefix.length in 3..8) {
            val fuzzyMatches = commonDictionary
                .asSequence()
                .filter { candidate ->
                    val lenDiff = kotlin.math.abs(lowerPrefix.length - candidate.length)
                    if (lenDiff > 1) return@filter false
                    val dist = levenshteinDistance(lowerPrefix, candidate)
                    dist <= 1 && !results.any { it.word.equals(candidate, ignoreCase = true) }
                }
                .take(2)
                .map { WordSuggestion(word = matchCase(cleanPrefix, it), score = 50, isAutoCorrect = true) }
                .toList()

            results.addAll(fuzzyMatches)
        }

        return results
            .filter { !it.word.contains(" ") && it.word.isNotBlank() }
            .distinctBy { it.word.lowercase() }
            .take(maxResults)
    }

    private fun findTriePrefixMatches(prefix: String): List<WordSuggestion> {
        var current = root
        for (char in prefix) {
            current = current.children[char] ?: return emptyList()
        }

        val matches = mutableListOf<Pair<String, Int>>()
        dfsTrie(current, prefix, matches, maxDepth = 6)

        return matches
            .sortedByDescending { it.second }
            .map { (word, score) -> WordSuggestion(word = matchCase(prefix, word), score = score) }
    }

    private fun dfsTrie(node: TrieNode, currentPath: String, results: MutableList<Pair<String, Int>>, maxDepth: Int) {
        if (results.size >= 8 || maxDepth <= 0) return
        if (node.isWord) {
            results.add(Pair(currentPath, node.frequency))
        }
        for ((char, child) in node.children) {
            dfsTrie(child, currentPath + char, results, maxDepth - 1)
        }
    }

    /**
     * Levenshtein Distance Algorithm
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun matchCase(original: String, target: String): String {
        return when {
            original.all { it.isUpperCase() } -> target.uppercase()
            original.firstOrNull()?.isUpperCase() == true -> target.replaceFirstChar { it.uppercase() }
            else -> target
        }
    }

    /**
     * Backspace gesture: Find character index for deleting entire word backward
     */
    fun findPreviousWordBoundary(text: String, selectionIndex: Int): Int {
        if (selectionIndex <= 0 || text.isEmpty()) return 0
        var idx = selectionIndex - 1
        // Skip spaces
        while (idx > 0 && text[idx].isWhitespace()) {
            idx--
        }
        // Skip letters
        while (idx > 0 && !text[idx - 1].isWhitespace()) {
            idx--
        }
        return idx
    }
}
