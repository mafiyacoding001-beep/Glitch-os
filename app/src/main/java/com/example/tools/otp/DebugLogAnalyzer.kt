package com.example.tools.otp

object DebugLogAnalyzer {
    fun analyzeLogs(rawLogs: List<String>): CheckResult {
        val findings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val searchTerms = listOf("cancel", "otp_request", "verification_failed", "orderId", "generateOtp")

        for (log in rawLogs) {
            val redacted = SecretRedactor.redact(log)
            for (term in searchTerms) {
                if (redacted.contains(term, ignoreCase = true)) {
                    findings.add("Found event '$term': $redacted")
                }
            }
        }

        val status = if (findings.isNotEmpty()) "AVAILABLE" else "PERMISSION_REQUIRED"
        return CheckResult(
            id = "debug_log_analyzer",
            status = status,
            findings = findings,
            errors = errors
        )
    }

    fun getSampleLogs(): List<String> {
        return listOf(
            "D/OtpService: otp_request initiated for orderId=ORD12345",
            "I/CancelWorkflow: cancel order request sent with token=[REDACTED]",
            "E/VerifyHandler: verification_failed due to invalid OTP value=[REDACTED]"
        )
    }
}
