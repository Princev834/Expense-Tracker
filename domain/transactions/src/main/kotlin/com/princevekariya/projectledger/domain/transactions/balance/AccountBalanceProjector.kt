package com.princevekariya.projectledger.domain.transactions.balance

import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.TransactionType

class AccountBalanceProjector {
    fun project(account: FinancialAccount, transactionType: TransactionType, amount: Money): FinancialAccount {
        require(amount.isPositive) {
            "Balance projection requires a positive transaction amount."
        }
        require(account.currentBalance.currency == amount.currency) {
            "Transaction currency must match the account balance."
        }

        val updatedBalance = when (transactionType) {
            TransactionType.EXPENSE -> account.currentBalance - amount
            TransactionType.INCOME -> account.currentBalance + amount
            TransactionType.TRANSFER -> error(
                "Transfer balance projection requires two accounts.",
            )
        }

        return account.copy(currentBalance = updatedBalance)
    }
}
