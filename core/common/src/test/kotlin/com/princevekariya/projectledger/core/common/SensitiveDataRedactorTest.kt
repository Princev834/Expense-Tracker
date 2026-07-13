package com.princevekariya.projectledger.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SensitiveDataRedactorTest {
    @Test
    fun `redactor removes email UPI and long numeric identifiers`() {
        val input = "Email prince@example.com UPI prince@okhdfcbank reference 1234567890"

        val result = SensitiveDataRedactor.redact(input)

        assertEquals(
            "Email [redacted-email] UPI [redacted-upi] reference [redacted-number]",
            result,
        )
        assertFalse(result.contains("prince@example.com"))
        assertFalse(result.contains("1234567890"))
    }

    @Test
    fun `ordinary short values remain readable`() {
        val input = "Expense draft accepted for category Food"

        assertEquals(input, SensitiveDataRedactor.redact(input))
    }
}
