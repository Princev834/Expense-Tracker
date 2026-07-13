package com.princevekariya.projectledger.core.common

object SensitiveDataRedactor {
    private val emailPattern = Regex(
        pattern = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}",
    )
    private val upiPattern = Regex(
        pattern = "[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}",
    )
    private val longNumberPattern = Regex(
        pattern = "(?<!\\d)\\d{6,}(?!\\d)",
    )

    fun redact(value: String): String = value
        .replace(emailPattern, REDACTED_EMAIL)
        .replace(upiPattern, REDACTED_UPI)
        .replace(longNumberPattern, REDACTED_NUMBER)

    private const val REDACTED_EMAIL = "[redacted-email]"
    private const val REDACTED_UPI = "[redacted-upi]"
    private const val REDACTED_NUMBER = "[redacted-number]"
}
