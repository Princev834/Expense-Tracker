package com.princevekariya.projectledger.navigation

import com.princevekariya.projectledger.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionEntryDestinationTest {
    @Test
    fun expenseAndIncomeRoutesUseStableLowercaseValues() {
        assertEquals(
            "transaction-entry/expense",
            TransactionEntryDestination.createRoute(
                type = TransactionType.EXPENSE,
            ),
        )
        assertEquals(
            "transaction-entry/income",
            TransactionEntryDestination.createRoute(
                type = TransactionType.INCOME,
            ),
        )
    }

    @Test
    fun parserDefaultsUnsupportedValuesToExpense() {
        assertEquals(
            TransactionType.INCOME,
            TransactionEntryDestination.parseType(value = "income"),
        )
        assertEquals(
            TransactionType.EXPENSE,
            TransactionEntryDestination.parseType(value = "unknown"),
        )
        assertEquals(
            TransactionType.EXPENSE,
            TransactionEntryDestination.parseType(value = null),
        )
    }
}
