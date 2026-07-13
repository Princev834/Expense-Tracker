package com.princevekariya.projectledger.core.common

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFacingErrorTest {
    @Test
    fun `security exception maps to permission guidance`() {
        val result = SecurityException().toUserFacingError()

        assertEquals(UserFacingError.PermissionRequired, result)
        assertTrue(result.canRetry)
    }

    @Test
    fun `IO exception maps to connection guidance`() {
        val result = IOException().toUserFacingError()

        assertEquals(UserFacingError.ConnectionUnavailable, result)
        assertTrue(result.canRetry)
    }

    @Test
    fun `illegal argument maps to non retryable input guidance`() {
        val result = IllegalArgumentException().toUserFacingError()

        assertEquals(UserFacingError.InvalidInput, result)
        assertFalse(result.canRetry)
    }

    @Test
    fun `unknown exception maps to safe generic guidance`() {
        val result = IllegalStateException("Internal database details").toUserFacingError()

        assertEquals(UserFacingError.Unexpected, result)
        assertFalse(result.message.contains("database", ignoreCase = true))
    }
}
