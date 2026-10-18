package com.example.ui.keyboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service to fetch single-word auto-suggestions from Google Suggest API.
 * Ensures strictly 1-word clean suggestions (no multi-word phrases or sentences).
 */
object GoogleSuggestService {

    suspend fun fetchGoogleSuggestions(query: String, lang: String = "en", maxResults: Int = 4): List<WordSuggestion> {
        val trimmed = query.trim()
        if (trimmed.length < 2 || trimmed.contains(" ")) return emptyList()

        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
                val urlString = "https://suggestqueries.google.com/complete/search?client=firefox&hl=$lang&q=$encodedQuery"
                val url = URL(urlString)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 1200
                    readTimeout = 1200
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val rootArray = JSONArray(responseText)
                    if (rootArray.length() >= 2) {
                        val suggestionsArray = rootArray.getJSONArray(1)
                        val wordSet = linkedSetOf<String>()

                        for (i in 0 until suggestionsArray.length()) {
                            val candidate = suggestionsArray.getString(i).trim()
                            if (candidate.isNotEmpty()) {
                                // Extract ONLY the first single word that starts with the query prefix
                                val singleWord = candidate.split(Regex("[\\s\\p{Punct}]+"))
                                    .firstOrNull { it.isNotBlank() }
                                    ?: candidate

                                val cleanWord = singleWord.trim()
                                if (cleanWord.length >= trimmed.length &&
                                    cleanWord.startsWith(trimmed, ignoreCase = true) &&
                                    !cleanWord.contains(" ")
                                ) {
                                    wordSet.add(cleanWord)
                                    if (wordSet.size >= maxResults) break
                                }
                            }
                        }

                        wordSet.map { word ->
                            WordSuggestion(
                                word = word,
                                isAutoCorrect = word.equals(trimmed, ignoreCase = true)
                            )
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                // Silently return empty on network timeout to avoid blocking or hanging UI
                emptyList()
            } finally {
                conn?.disconnect()
            }
        }
    }
}
