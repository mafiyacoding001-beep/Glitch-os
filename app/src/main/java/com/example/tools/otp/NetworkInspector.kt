package com.example.tools.otp

data class ApiInspectionRecord(
    val endpoint: String,
    val method: String,
    val responseStatus: Int,
    val durationMs: Long,
    val timestamp: String
)

object NetworkInspector {
    fun inspectNetworkCalls(): CheckResult {
        val sampleCalls = listOf(
            ApiInspectionRecord("https://staging.api.delivery/v1/order/cancel", "POST", 200, 142, "2026-10-14T20:30:00Z"),
            ApiInspectionRecord("https://staging.api.delivery/v1/otp/generate", "POST", 200, 210, "2026-10-14T20:30:15Z"),
            ApiInspectionRecord("https://staging.api.delivery/v1/otp/verify", "POST", 400, 95, "2026-10-14T20:30:45Z")
        )

        val findings = sampleCalls.map { call ->
            SecretRedactor.redact("[${call.method}] ${call.endpoint} -> Status: ${call.responseStatus} (${call.durationMs}ms)")
        }

        return CheckResult(
            id = "network_inspector",
            status = "AVAILABLE",
            findings = findings,
            errors = emptyList()
        )
    }
}
