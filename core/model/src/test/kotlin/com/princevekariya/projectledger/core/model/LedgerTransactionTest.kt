package com.princevekariya.projectledger.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LedgerTransactionTest {
    @Test
    fun `expense requires a positive amount and category`() {
        val transaction = LedgerTransaction(
            id = "transaction-1",
            type = TransactionType.EXPENSE,
            amount = Money.fromMajorUnits("120"),
            accountId = "cash-account",
            occurredAtEpochMillis = 1_000L,
            paymentMethod = PaymentMethod.CASH,
            categoryId = "food-category",
        )

        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals(12_000L, transaction.amount.minorUnits)
    }

    @Test
    fun `expense without category is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            LedgerTransaction(
                id = "transaction-2",
                type = TransactionType.EXPENSE,
                amount = Money.fromMajorUnits("50"),
                accountId = "cash-account",
                occurredAtEpochMillis = 2_000L,
                paymentMethod = PaymentMethod.CASH,
            )
        }
    }

    @Test
    fun `transfer requires two different accounts`() {
        assertThrows(IllegalArgumentException::class.java) {
            LedgerTransaction(
                id = "transaction-3",
                type = TransactionType.TRANSFER,
                amount = Money.fromMajorUnits("1,000"),
                accountId = "bank-account",
                destinationAccountId = "bank-account",
                occurredAtEpochMillis = 3_000L,
                paymentMethod = PaymentMethod.BANK_TRANSFER,
            )
        }
    }

    @Test
    fun `valid transfer has no category or merchant`() {
        val transaction = LedgerTransaction(
            id = "transaction-4",
            type = TransactionType.TRANSFER,
            amount = Money.fromMajorUnits("1,000"),
            accountId = "bank-account",
            destinationAccountId = "cash-account",
            occurredAtEpochMillis = 4_000L,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
        )

        assertEquals("cash-account", transaction.destinationAccountId)
        assertEquals(null, transaction.categoryId)
    }
}
