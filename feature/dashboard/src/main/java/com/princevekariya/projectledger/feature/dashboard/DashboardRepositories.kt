package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository

data class DashboardRepositories(
    val accounts: AccountRepository,
    val transactions: TransactionRepository,
    val categories: CategoryRepository,
    val merchants: MerchantRepository,
)
