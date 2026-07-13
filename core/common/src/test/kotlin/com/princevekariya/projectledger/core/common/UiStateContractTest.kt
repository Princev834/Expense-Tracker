package com.princevekariya.projectledger.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UiStateContractTest {
    @Test
    fun `error state preserves its user-readable message`() {
        val state = UiLoadState.Error(message = "Unable to load transactions")

        assertEquals("Unable to load transactions", state.message)
    }

    @Test
    fun `blank error messages are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            UiLoadState.Error(message = "   ")
        }
    }

    @Test
    fun `blank one-off messages are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            UiMessage(id = 1L, text = "")
        }
    }
}
