package com.princevekariya.projectledger.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FinancialModelValidationTest {
    @Test
    fun `account keeps exact opening and current balances`() {
        val account = FinancialAccount(
            id = "cash-account",
            name = "Cash",
            type = AccountType.CASH,
            openingBalance = Money.fromMajorUnits("500"),
        )

        assertEquals(account.openingBalance, account.currentBalance)
    }

    @Test
    fun `budget requires a positive limit and valid period`() {
        val budget = Budget(
            id = "food-budget",
            name = "Monthly food",
            limit = Money.fromMajorUnits("5,000"),
            period = BudgetPeriod.MONTHLY,
            startEpochMillis = 1_000L,
            endEpochMillis = 2_000L,
            categoryId = "food-category",
        )

        assertEquals(500_000L, budget.limit.minorUnits)
    }

    @Test
    fun `budget rejects an invalid date range`() {
        assertThrows(IllegalArgumentException::class.java) {
            Budget(
                id = "invalid-budget",
                name = "Invalid",
                limit = Money.fromMajorUnits("100"),
                period = BudgetPeriod.CUSTOM,
                startEpochMillis = 2_000L,
                endEpochMillis = 1_000L,
            )
        }
    }

    @Test
    fun `merchant exposes a normalized search key`() {
        val merchant = Merchant(
            id = "merchant-1",
            name = "  College Canteen  ",
        )

        assertEquals("college canteen", merchant.searchKey)
    }
}
