package com.princevekariya.projectledger.domain.transactions.balance

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountBalanceProjectorTest {
    private val projector = AccountBalanceProjector()

    @Test
    fun expenseReducesCurrentBalanceWithoutChangingOpeningBalance() {
        val account = account(
            openingMinorUnits = 50_000L,
            currentMinorUnits = 40_000L,
        )

        val updated = projector.project(
            account = account,
            transactionType = TransactionType.EXPENSE,
            amount = Money(minorUnits = 12_500L),
        )

        assertEquals(50_000L, updated.openingBalance.minorUnits)
        assertEquals(27_500L, updated.currentBalance.minorUnits)
    }

    @Test
    fun incomeIncreasesCurrentBalance() {
        val updated = projector.project(
            account = account(
                openingMinorUnits = 0L,
                currentMinorUnits = 5_000L,
            ),
            transactionType = TransactionType.INCOME,
            amount = Money(minorUnits = 20_000L),
        )

        assertEquals(25_000L, updated.currentBalance.minorUnits)
    }

    @Test
    fun transferRequiresTheDedicatedTwoAccountFlow() {
        val failure = runCatching {
            projector.project(
                account = account(
                    openingMinorUnits = 0L,
                    currentMinorUnits = 0L,
                ),
                transactionType = TransactionType.TRANSFER,
                amount = Money(minorUnits = 100L),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
    }

    private fun account(openingMinorUnits: Long, currentMinorUnits: Long): FinancialAccount = FinancialAccount(
        id = "account-cash",
        name = "Cash",
        type = AccountType.CASH,
        openingBalance = Money(minorUnits = openingMinorUnits),
        currentBalance = Money(minorUnits = currentMinorUnits),
    )
}
