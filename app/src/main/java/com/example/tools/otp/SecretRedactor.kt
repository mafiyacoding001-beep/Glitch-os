package com.example.tools.otp

object SecretRedactor {
    private val sensitivePatterns = listOf(
        "otp".toRegex(RegexOption.IGNORE_CASE),
        "password".toRegex(RegexOption.IGNORE_CASE),
        "token".toRegex(RegexOption.IGNORE_CASE),
        "authorization".toRegex(RegexOption.IGNORE_CASE),
        "cookie".toRegex(RegexOption.IGNORE_CASE),
        "secret".toRegex(RegexOption.IGNORE_CASE)
    )

    fun redact(input: String): String {
        var redacted = input
        for (pattern in sensitivePatterns) {
            if (pattern.containsMatchIn(redacted)) {
                redacted = redacted.replace(pattern, "[REDACTED]")
            }
        }
        return redacted
    }
}
