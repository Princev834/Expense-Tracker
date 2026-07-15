package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.model.TransactionType

enum class TransactionHistoryFilter {
    ALL,
    EXPENSE,
    INCOME,
    ;

    fun accepts(type: TransactionType): Boolean = when (this) {
        ALL -> true
        EXPENSE -> type == TransactionType.EXPENSE
        INCOME -> type == TransactionType.INCOME
    }

    fun displayLabel(): String = when (this) {
        ALL -> "All"
        EXPENSE -> "Expense"
        INCOME -> "Income"
    }
}
