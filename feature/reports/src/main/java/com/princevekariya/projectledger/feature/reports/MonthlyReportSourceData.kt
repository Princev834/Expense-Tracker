package com.princevekariya.projectledger.feature.reports

import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.TransactionCategory

internal data class MonthlyReportSourceData(
    val transactions: List<LedgerTransaction>,
    val expenseCategories: List<TransactionCategory>,
)
