package com.example.ui.keyboard

object HinglishTransliteration {

    private val commonHinglishMap = mapOf(
        "namaste" to "नमस्ते",
        "kaise" to "कैसे",
        "ho" to "हो",
        "kya" to "क्या",
        "hai" to "है",
        "ha" to "हाँ",
        "haan" to "हाँ",
        "nahi" to "नहीं",
        "shukriya" to "शुक्रिया",
        "dhanyawad" to "धन्यवाद",
        "dost" to "दोस्त",
        "bhai" to "भाई",
        "sab" to "सब",
        "thik" to "ठीक",
        "karo" to "करो",
        "karna" to "करना",
        "aaj" to "आज",
        "kal" to "कल",
        "ghar" to "घर",
        "badi" to "बड़ी",
        "achha" to "अच्छा",
        "bahut" to "बहुत",
        "khush" to "खुश",
        "samajh" to "समझ",
        "kaam" to "काम",
        "bolo" to "बोलो",
        "sun" to "सुन",
        "dekho" to "देखो",
        "chalo" to "चलो",
        "pani" to "पानी",
        "khana" to "खाना",
        "raha" to "रहा",
        "rahe" to "रहे",
        "gaya" to "गया",
        "hoga" to "होगा",
        "baat" to "बात"
    )

    fun transliterate(roman: String): String? {
        val lower = roman.lowercase().trim()
        return commonHinglishMap[lower]
    }

    /**
     * Returns strictly single-word suggestions (e.g. "नमस्ते", "कैसे").
     * No multi-word phrases, sentences, or brackets.
     */
    fun getHinglishSuggestions(romanPrefix: String): List<WordSuggestion> {
        if (romanPrefix.isBlank() || romanPrefix.contains(" ")) return emptyList()
        val lower = romanPrefix.lowercase().trim()
        val results = mutableListOf<WordSuggestion>()

        val directMatch = commonHinglishMap[lower]
        if (directMatch != null) {
            results.add(WordSuggestion(word = directMatch, score = 100))
        }

        commonHinglishMap.entries
            .filter { it.key.startsWith(lower) && it.key != lower }
            .take(2)
            .forEach { (key, hindiVal) ->
                results.add(WordSuggestion(word = hindiVal, score = 80))
            }

        return results
    }
}
