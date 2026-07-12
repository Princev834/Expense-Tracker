package com.princevekariya.projectledger.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConstantsTest {
    @Test
    fun defaultCurrency_isIndianRupee() {
        assertEquals("INR", ProjectConstants.DEFAULT_CURRENCY_CODE)
    }
}
