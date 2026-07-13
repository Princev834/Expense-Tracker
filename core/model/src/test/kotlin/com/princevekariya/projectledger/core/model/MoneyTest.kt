package com.princevekariya.projectledger.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun `major unit parsing stores exact minor units`() {
        val money = Money.fromMajorUnits("12,500.75")

        assertEquals(1_250_075L, money.minorUnits)
        assertEquals(CurrencyCode.INR, money.currency)
        assertEquals("INR 12,500.75", money.formatted())
    }

    @Test
    fun `fractional values beyond currency precision are rejected`() {
        val result = Money.parseMajorUnits("10.999")

        assertTrue(result.isFailure)
    }

    @Test
    fun `money arithmetic remains exact`() {
        val income = Money.fromMajorUnits("100.50")
        val expense = Money.fromMajorUnits("25.25")

        assertEquals(Money.fromMajorUnits("125.75"), income + expense)
        assertEquals(Money.fromMajorUnits("75.25"), income - expense)
    }

    @Test
    fun `money exposes useful sign information`() {
        val negative = Money(minorUnits = -500L)

        assertTrue(negative.isNegative)
        assertFalse(negative.isPositive)
        assertEquals(Money(minorUnits = 500L), negative.absoluteValue())
    }
}
