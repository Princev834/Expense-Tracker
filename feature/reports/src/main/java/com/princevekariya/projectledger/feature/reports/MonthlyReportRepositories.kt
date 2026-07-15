package com.princevekariya.projectledger.feature.reports

import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository

data class MonthlyReportRepositories(
    val transactions: TransactionRepository,
    val categories: CategoryRepository,
)
