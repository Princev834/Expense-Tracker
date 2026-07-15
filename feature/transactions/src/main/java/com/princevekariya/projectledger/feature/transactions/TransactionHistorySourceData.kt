package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.core.model.TransactionCategory

internal data class TransactionHistorySourceData(
    val accounts: List<FinancialAccount>,
    val transactions: List<LedgerTransaction>,
    val expenseCategories: List<TransactionCategory>,
    val incomeCategories: List<TransactionCategory>,
    val merchants: List<Merchant>,
)
