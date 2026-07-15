package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository

data class TransactionHistoryRepositories(
    val accounts: AccountRepository,
    val transactions: TransactionRepository,
    val categories: CategoryRepository,
    val merchants: MerchantRepository,
)
