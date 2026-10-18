package com.example.ui.keyboard

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

enum class TextTone {
    PROFESSIONAL,
    CASUAL,
    CONCISE,
    EMOJIFY,
    VIRAL_HYPE,
    URGENT,
    FIX_GRAMMAR,
    ROAST,
    SHAKESPEARE
}

enum class CaseType {
    UPPERCASE,
    LOWERCASE,
    TITLE_CASE,
    CAMEL_CASE,
    PASCAL_CASE,
    SNAKE_CASE,
    KEBAB_CASE,
    SPONGEBOB_MOCKING,
    REVERSE
}

enum class FancyFontStyle {
    GOTHIC,
    SCRIPT_CURSIVE,
    CIRCLED_BUBBLE,
    BLACK_BUBBLE,
    DOUBLE_STRUCK,
    MONOSPACE,
    SQUARE,
    STRIKETHROUGH,
    UNDERLINE,
    UPSIDE_DOWN
}

object KeyboardTextTransformers {

    fun transformTone(text: String, tone: TextTone): String {
        if (text.isBlank()) return text
        val trimmed = text.trim()
        return when (tone) {
            TextTone.PROFESSIONAL -> toProfessional(trimmed)
            TextTone.CASUAL -> toCasual(trimmed)
            TextTone.CONCISE -> toConcise(trimmed)
            TextTone.EMOJIFY -> toEmojify(trimmed)
            TextTone.VIRAL_HYPE -> toViralHype(trimmed)
            TextTone.URGENT -> toUrgent(trimmed)
            TextTone.FIX_GRAMMAR -> fixGrammarAndPolish(trimmed)
            TextTone.ROAST -> toRoast(trimmed)
            TextTone.SHAKESPEARE -> toShakespeare(trimmed)
        }
    }

    private fun toProfessional(text: String): String {
        var result = text
        val replacements = mapOf(
            "\\bhi\\b" to "Greetings,",
            "\\bhey\\b" to "Dear Colleagues,",
            "\\bthanks\\b" to "Thank you for your valuable time and cooperation.",
            "\\bgood\\b" to "exemplary",
            "\\bbad\\b" to "sub-optimal",
            "\\bhelp\\b" to "assist further with this matter",
            "\\bsoon\\b" to "at your earliest convenience",
            "\\bwait\\b" to "Please allow us some time to review",
            "\\bokay\\b" to "Duly noted and acknowledged.",
            "\\bok\\b" to "Duly noted."
        )
        for ((k, v) in replacements) {
            result = result.replace(Regex(k, RegexOption.IGNORE_CASE), v)
        }
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) {
            result += "."
        }
        return "Dear Team,\n\n$result\n\nKind regards,\n"
    }

    private fun toCasual(text: String): String {
        var result = text
        val replacements = mapOf(
            "\\byes\\b" to "yep fr",
            "\\bno\\b" to "nah",
            "\\bhello\\b" to "yo what's up",
            "\\bvery good\\b" to "straight fire 🔥",
            "\\bgood\\b" to "vibes",
            "\\bcool\\b" to "dope",
            "\\bthanks\\b" to "tysm bro",
            "\\breally\\b" to "lowkey fr"
        )
        for ((k, v) in replacements) {
            result = result.replace(Regex(k, RegexOption.IGNORE_CASE), v)
        }
        return "$result fr no cap 🚀"
    }

    private fun toConcise(text: String): String {
        val sentences = text.split(". ", "? ", "! ", "\n")
            .filter { it.isNotBlank() }
            .map { "• ${it.trim().replaceFirstChar { c -> c.uppercase() }}" }
        return if (sentences.isNotEmpty()) sentences.joinToString("\n") else "• $text"
    }

    private fun toEmojify(text: String): String {
        val emojiMap = mapOf(
            "fire" to "🔥", "lit" to "🔥", "love" to "❤️", "heart" to "💖",
            "good" to "✨", "great" to "🌟", "cool" to "😎", "money" to "💰",
            "crypto" to "🪙", "code" to "💻", "glitch" to "⚡", "power" to "⚡",
            "fast" to "🚀", "speed" to "⚡", "keyboard" to "⌨️", "smart" to "🧠",
            "idea" to "💡", "check" to "✅", "win" to "🏆", "party" to "🎉",
            "yes" to "👍", "no" to "❌", "star" to "⭐", "phone" to "📱"
        )
        val words = text.split(" ").map { word ->
            val clean = word.lowercase().replace(Regex("[^a-z0-9]"), "")
            val emoji = emojiMap[clean]
            if (emoji != null) "$word $emoji" else word
        }
        return words.joinToString(" ") + " ✨🚀"
    }

    private fun toViralHype(text: String): String {
        return "🚨 BREAKING: $text 🚨\n\nRetweet and bookmark this right now! 🔥👇"
    }

    private fun toUrgent(text: String): String {
        return "⚠️ HIGH PRIORITY / URGENT ⚠️\n$text\n\nPlease act upon this immediately!"
    }

    private fun fixGrammarAndPolish(text: String): String {
        var polished = text.replace(Regex("\\s+"), " ").trim()
        polished = polished.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        polished = polished.replace(Regex("\\bi\\b"), "I")
        polished = polished.replace(Regex("\\bdont\\b", RegexOption.IGNORE_CASE), "don't")
        polished = polished.replace(Regex("\\bcant\\b", RegexOption.IGNORE_CASE), "can't")
        polished = polished.replace(Regex("\\bwont\\b", RegexOption.IGNORE_CASE), "won't")
        polished = polished.replace(Regex("\\bim\\b", RegexOption.IGNORE_CASE), "I'm")
        polished = polished.replace(Regex("\\bteh\\b", RegexOption.IGNORE_CASE), "the")
        polished = polished.replace(Regex("\\brecieve\\b", RegexOption.IGNORE_CASE), "receive")
        polished = polished.replace(Regex("\\bseperate\\b", RegexOption.IGNORE_CASE), "separate")
        if (!polished.endsWith(".") && !polished.endsWith("!") && !polished.endsWith("?")) {
            polished += "."
        }
        return polished
    }

    private fun toRoast(text: String): String {
        return "Bro really typed: \"$text\" 💀... couldn't be me."
    }

    private fun toShakespeare(text: String): String {
        var result = text
        val replacements = mapOf(
            "\\byou\\b" to "thou",
            "\\byour\\b" to "thy",
            "\\bare\\b" to "art",
            "\\bhave\\b" to "hast",
            "\\bdo\\b" to "dost",
            "\\bdoes\\b" to "doth",
            "\\bbefore\\b" to "ere",
            "\\bwhy\\b" to "wherefore"
        )
        for ((k, v) in replacements) {
            result = result.replace(Regex(k, RegexOption.IGNORE_CASE), v)
        }
        return "Hark! $result, verily."
    }

    fun convertCase(text: String, caseType: CaseType): String {
        if (text.isBlank()) return text
        return when (caseType) {
            CaseType.UPPERCASE -> text.uppercase()
            CaseType.LOWERCASE -> text.lowercase()
            CaseType.TITLE_CASE -> text.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            CaseType.CAMEL_CASE -> {
                val words = text.split(Regex("[\\s_\\-]+")).filter { it.isNotEmpty() }
                if (words.isEmpty()) "" else {
                    words.first().lowercase() + words.drop(1).joinToString("") {
                        it.lowercase().replaceFirstChar { c -> c.uppercase() }
                    }
                }
            }
            CaseType.PASCAL_CASE -> {
                val words = text.split(Regex("[\\s_\\-]+")).filter { it.isNotEmpty() }
                words.joinToString("") {
                    it.lowercase().replaceFirstChar { c -> c.uppercase() }
                }
            }
            CaseType.SNAKE_CASE -> {
                text.trim().split(Regex("[\\s_\\-]+")).filter { it.isNotEmpty() }.joinToString("_") { it.lowercase() }
            }
            CaseType.KEBAB_CASE -> {
                text.trim().split(Regex("[\\s_\\-]+")).filter { it.isNotEmpty() }.joinToString("-") { it.lowercase() }
            }
            CaseType.SPONGEBOB_MOCKING -> {
                text.mapIndexed { index, c ->
                    if (index % 2 == 0) c.lowercaseChar() else c.uppercaseChar()
                }.joinToString("")
            }
            CaseType.REVERSE -> text.reversed()
        }
    }

    fun applyFancyFont(text: String, style: FancyFontStyle): String {
        if (text.isBlank()) return text
        return when (style) {
            FancyFontStyle.GOTHIC -> mapUnicode(text, GOTHIC_MAP)
            FancyFontStyle.SCRIPT_CURSIVE -> mapUnicode(text, SCRIPT_MAP)
            FancyFontStyle.CIRCLED_BUBBLE -> mapUnicode(text, CIRCLED_MAP)
            FancyFontStyle.BLACK_BUBBLE -> mapUnicode(text, BLACK_BUBBLE_MAP)
            FancyFontStyle.DOUBLE_STRUCK -> mapUnicode(text, DOUBLE_STRUCK_MAP)
            FancyFontStyle.MONOSPACE -> mapUnicode(text, MONOSPACE_MAP)
            FancyFontStyle.SQUARE -> mapUnicode(text, SQUARE_MAP)
            FancyFontStyle.STRIKETHROUGH -> text.map { "$it\u0336" }.joinToString("")
            FancyFontStyle.UNDERLINE -> text.map { "$it\u0332" }.joinToString("")
            FancyFontStyle.UPSIDE_DOWN -> {
                val upsideDownMap = mapOf(
                    'a' to "ɐ", 'b' to "q", 'c' to "ɔ", 'd' to "p", 'e' to "ǝ", 'f' to "ɟ", 'g' to "ƃ",
                    'h' to "ɥ", 'i' to "ᴉ", 'j' to "ɾ", 'k' to "ʞ", 'l' to "l", 'm' to "ɯ", 'n' to "u",
                    'o' to "o", 'p' to "d", 'q' to "b", 'r' to "ɹ", 's' to "s", 't' to "ʇ", 'u' to "n",
                    'v' to "ʌ", 'w' to "ʍ", 'x' to "x", 'y' to "ʎ", 'z' to "z",
                    'A' to "∀", 'B' to "𐐒", 'C' to "Ɔ", 'D' to "ᗡ", 'E' to "Ǝ", 'F' to "Ⅎ", 'G' to "⅁",
                    'H' to "H", 'I' to "I", 'J' to "ſ", 'K' to "ʞ", 'L' to "˥", 'M' to "W", 'N' to "N",
                    'O' to "O", 'P' to "Ԁ", 'Q' to "Ό", 'R' to "ᴚ", 'S' to "S", 'T' to "⊥", 'U' to "∩",
                    'V' to "Λ", 'W' to "M", 'X' to "X", 'Y' to "⅄", 'Z' to "Z",
                    '1' to "Ɩ", '2' to "ᄅ", '3' to "Ɛ", '4' to "ㄣ", '5' to "ϛ", '6' to "9", '7' to "ㄥ",
                    '8' to "8", '9' to "6", '0' to "0", '?' to "¿", '!' to "¡", '.' to "˙", ',' to "\'"
                )
                text.reversed().map { upsideDownMap[it] ?: it.toString() }.joinToString("")
            }
        }
    }

    private class MathParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        init {
            nextChar()
        }

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code) {
                while (ch in 'a'.code..'z'.code) nextChar()
                val func = str.substring(startPos, pos)
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else {
                    x = parseFactor()
                }
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    "abs" -> abs(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }

    fun evaluateMathExpression(rawExpr: String): Pair<Double?, String> {
        if (rawExpr.isBlank()) return Pair(null, "")
        try {
            val sanitized = rawExpr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "3.141592653589793")
                .replace("e", "2.718281828459045")
                .replace(" ", "")

            val result = MathParser(sanitized).parse()

            val formatted = if (result.isNaN() || result.isInfinite()) {
                "Error"
            } else if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
            }
            return Pair(result, formatted)
        } catch (e: Exception) {
            return Pair(null, "Error")
        }
    }

    fun resolveSnippetPlaceholders(template: String, clipboardText: String? = null): String {
        val now = Date()
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

        val otp = Random.nextInt(100000, 999999).toString()
        val uuid = UUID.randomUUID().toString().take(8)

        return template
            .replace("{DATE}", dateFormat.format(now))
            .replace("{TIME}", timeFormat.format(now))
            .replace("{YEAR}", yearFormat.format(now))
            .replace("{RANDOM_OTP}", otp)
            .replace("{UUID}", uuid)
            .replace("{CLIPBOARD}", clipboardText ?: "")
    }

    private fun mapUnicode(text: String, map: Map<Char, String>): String {
        return text.map { map[it] ?: it.toString() }.joinToString("")
    }

    private val GOTHIC_MAP = mapOf(
        'a' to "𝖆", 'b' to "𝖇", 'c' to "𝖈", 'd' to "𝖉", 'e' to "𝖊", 'f' to "𝖋", 'g' to "𝖌",
        'h' to "𝖍", 'i' to "𝖎", 'j' to "𝖏", 'k' to "𝖐", 'l' to "𝖑", 'm' to "𝖒", 'n' to "𝖓",
        'o' to "𝖔", 'p' to "𝖕", 'q' to "𝖖", 'r' to "𝖗", 's' to "𝖘", 't' to "𝖙", 'u' to "𝖚",
        'v' to "𝖛", 'w' to "𝖜", 'x' to "𝖝", 'y' to "𝖞", 'z' to "𝖟",
        'A' to "𝕬", 'B' to "𝕭", 'C' to "𝕮", 'D' to "𝕯", 'E' to "𝕰", 'F' to "𝕱", 'G' to "𝕲",
        'H' to "𝕳", 'I' to "𝕴", 'J' to "𝕵", 'K' to "𝕶", 'L' to "𝕷", 'M' to "𝕸", 'N' to "𝕹",
        'O' to "𝕺", 'P' to "𝕻", 'Q' to "𝕼", 'R' to "𝕽", 'S' to "𝕾", 'T' to "𝕿", 'U' to "𝖀",
        'V' to "𝖁", 'W' to "𝖂", 'X' to "𝖃", 'Y' to "𝖄", 'Z' to "𝖅"
    )

    private val SCRIPT_MAP = mapOf(
        'a' to "𝒶", 'b' to "𝒷", 'c' to "𝒸", 'd' to "𝒹", 'e' to "ℯ", 'f' to "𝒻", 'g' to "ℊ",
        'h' to "𝒽", 'i' to "𝒾", 'j' to "𝒿", 'k' to "𝓀", 'l' to "𝓁", 'm' to "𝓂", 'n' to "𝓃",
        'o' to "ℴ", 'p' to "𝓅", 'q' to "𝓆", 'r' to "𝓇", 's' to "𝓈", 't' to "𝓉", 'u' to "𝓊",
        'v' to "𝓋", 'w' to "𝓌", 'x' to "𝓍", 'y' to "𝓎", 'z' to "𝓏",
        'A' to "𝒜", 'B' to "ℬ", 'C' to "𝒞", 'D' to "𝒟", 'E' to "ℰ", 'F' to "ℱ", 'G' to "𝒢",
        'H' to "ℋ", 'I' to "ℐ", 'J' to "𝒥", 'K' to "𝒦", 'L' to "ℒ", 'M' to "ℳ", 'N' to "𝒩",
        'O' to "𝒪", 'P' to "𝒫", 'Q' to "𝒬", 'R' to "ℛ", 'S' to "𝒮", 'T' to "𝒯", 'U' to "𝒰",
        'V' to "𝒱", 'W' to "𝒲", 'X' to "𝒳", 'Y' to "𝒴", 'Z' to "𝒵"
    )

    private val CIRCLED_MAP = mapOf(
        'a' to "ⓐ", 'b' to "ⓑ", 'c' to "ⓒ", 'd' to "ⓓ", 'e' to "ⓔ", 'f' to "ⓕ", 'g' to "ⓖ",
        'h' to "ⓗ", 'i' to "ⓘ", 'j' to "ⓙ", 'k' to "ⓚ", 'l' to "ⓛ", 'm' to "ⓜ", 'n' to "ⓝ",
        'o' to "ⓞ", 'p' to "ⓟ", 'q' to "ⓠ", 'r' to "ⓡ", 's' to "ⓢ", 't' to "ⓣ", 'u' to "ⓤ",
        'v' to "ⓥ", 'w' to "ⓦ", 'x' to "ⓧ", 'y' to "ⓨ", 'z' to "ⓩ",
        'A' to "Ⓐ", 'B' to "Ⓑ", 'C' to "Ⓒ", 'D' to "Ⓓ", 'E' to "Ⓔ", 'F' to "Ⓕ", 'G' to "Ⓖ",
        'H' to "Ⓗ", 'I' to "Ⓘ", 'J' to "Ⓙ", 'K' to "Ⓚ", 'L' to "Ⓛ", 'M' to "Ⓜ", 'N' to "Ⓝ",
        'O' to "Ⓞ", 'P' to "Ⓟ", 'Q' to "Ⓠ", 'R' to "Ⓡ", 'S' to "Ⓢ", 'T' to "Ⓣ", 'U' to "Ⓤ",
        'V' to "Ⓥ", 'W' to "Ⓦ", 'X' to "Ⓧ", 'Y' to "Ⓨ", 'Z' to "Ⓩ",
        '0' to "⓪", '1' to "①", '2' to "②", '3' to "③", '4' to "④", '5' to "⑤", '6' to "⑥", '7' to "⑦", '8' to "⑧", '9' to "⑨"
    )

    private val BLACK_BUBBLE_MAP = mapOf(
        'a' to "🅐", 'b' to "🅑", 'c' to "🅒", 'd' to "🅓", 'e' to "🅔", 'f' to "🅕", 'g' to "🅖",
        'h' to "🅗", 'i' to "🅘", 'j' to "🅙", 'k' to "🅚", 'l' to "🅛", 'm' to "🅜", 'n' to "🅝",
        'o' to "🅞", 'p' to "🅟", 'q' to "🅠", 'r' to "🅡", 's' to "🅢", 't' to "🅣", 'u' to "🅤",
        'v' to "🅥", 'w' to "🅦", 'x' to "🅧", 'y' to "🅨", 'z' to "🅩",
        'A' to "🅐", 'B' to "🅑", 'C' to "🅒", 'D' to "🅓", 'E' to "🅔", 'F' to "🅕", 'G' to "🅖",
        'H' to "🅗", 'I' to "🅘", 'J' to "🅙", 'K' to "🅚", 'L' to "🅛", 'M' to "🅜", 'N' to "🅝",
        'O' to "🅞", 'P' to "🅟", 'Q' to "🅠", 'R' to "🅡", 'S' to "🅢", 'T' to "🅣", 'U' to "🅤",
        'V' to "🅥", 'W' to "🅦", 'X' to "🅧", 'Y' to "🅨", 'Z' to "🅩",
        '0' to "⓿", '1' to "❶", '2' to "❷", '3' to "❸", '4' to "❹", '5' to "❺", '6' to "❻", '7' to "❼", '8' to "❽", '9' to "❾"
    )

    private val DOUBLE_STRUCK_MAP = mapOf(
        'a' to "𝕒", 'b' to "𝕓", 'c' to "𝕔", 'd' to "𝕕", 'e' to "𝕖", 'f' to "𝕗", 'g' to "𝕘",
        'h' to "𝕙", 'i' to "𝕚", 'j' to "𝕛", 'k' to "𝕜", 'l' to "𝕝", 'm' to "𝕞", 'n' to "𝕟",
        'o' to "𝕠", 'p' to "𝕡", 'q' to "𝕢", 'r' to "𝕣", 's' to "𝕤", 't' to "𝕥", 'u' to "𝕦",
        'v' to "𝕧", 'w' to "𝕨", 'x' to "𝕩", 'y' to "𝕪", 'z' to "𝕫",
        'A' to "𝔸", 'B' to "𝔹", 'C' to "ℂ", 'D' to "𝔻", 'E' to "𝔼", 'F' to "𝔽", 'G' to "𝔾",
        'H' to "ℍ", 'I' to "𝕀", 'J' to "𝕁", 'K' to "𝕂", 'L' to "𝕃", 'M' to "𝕄", 'N' to "ℕ",
        'O' to "𝕆", 'P' to "ℙ", 'Q' to "ℚ", 'R' to "ℝ", 'S' to "𝕊", 'T' to "𝕋", 'U' to "𝕌",
        'V' to "𝕍", 'W' to "𝕎", 'X' to "𝕏", 'Y' to "𝕐", 'Z' to "ℤ",
        '0' to "𝟘", '1' to "𝟙", '2' to "𝟚", '3' to "𝟛", '4' to "𝟜", '5' to "𝟝", '6' to "𝟞", '7' to "𝟟", '8' to "𝟠", '9' to "𝟡"
    )

    private val MONOSPACE_MAP = mapOf(
        'a' to "𝚊", 'b' to "𝚋", 'c' to "𝚌", 'd' to "𝚍", 'e' to "𝚎", 'f' to "𝚏", 'g' to "𝚐",
        'h' to "𝚑", 'i' to "𝚒", 'j' to "𝚓", 'k' to "𝚔", 'l' to "𝚕", 'm' to "𝚖", 'n' to "𝚗",
        'o' to "𝚘", 'p' to "𝚙", 'q' to "𝚚", 'r' to "𝚛", 's' to "𝚜", 't' to "𝚝", 'u' to "𝚞",
        'v' to "𝚟", 'w' to "𝚠", 'x' to "𝚡", 'y' to "𝚢", 'z' to "𝚣",
        'A' to "𝙰", 'B' to "𝙱", 'C' to "𝙲", 'D' to "𝙳", 'E' to "𝙴", 'F' to "𝙵", 'G' to "𝙶",
        'H' to "𝙷", 'I' to "𝙸", 'J' to "𝙹", 'K' to "𝙺", 'L' to "𝙻", 'M' to "𝙼", 'N' to "𝙽",
        'O' to "𝙾", 'P' to "𝙿", 'Q' to "𝚀", 'R' to "𝚁", 'S' to "𝚂", 'T' to "𝚃", 'U' to "𝚄",
        'V' to "𝚅", 'W' to "𝚆", 'X' to "𝚇", 'Y' to "𝚈", 'Z' to "𝚉"
    )

    private val SQUARE_MAP = mapOf(
        'a' to "🄰", 'b' to "🄱", 'c' to "🄲", 'd' to "🄳", 'e' to "🄴", 'f' to "🄵", 'g' to "🄶",
        'h' to "🄷", 'i' to "🄸", 'j' to "🄹", 'k' to "🄺", 'l' to "🄻", 'm' to "🄼", 'n' to "🄽",
        'o' to "🄾", 'p' to "🄿", 'q' to "🅀", 'r' to "🅁", 's' to "🅂", 't' to "🅃", 'u' to "🅄",
        'v' to "🅅", 'w' to "🅆", 'x' to "🅇", 'y' to "🅈", 'z' to "🅉",
        'A' to "🄰", 'B' to "🄱", 'C' to "🄲", 'D' to "🄳", 'E' to "🄴", 'F' to "🄵", 'G' to "🄶",
        'H' to "🄷", 'I' to "🄸", 'J' to "🄹", 'K' to "🄺", 'L' to "🄻", 'M' to "🄼", 'N' to "🄽",
        'O' to "🄾", 'P' to "🄿", 'Q' to "🅀", 'R' to "🅁", 'S' to "🅂", 'T' to "🅃", 'U' to "🅄",
        'V' to "🅅", 'W' to "🅆", 'X' to "🅇", 'Y' to "🅈", 'Z' to "🅉"
    )
}
