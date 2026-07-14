package com.princevekariya.projectledger.domain.transactions.repository

import com.princevekariya.projectledger.core.model.FinancialAccount
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<FinancialAccount>>

    suspend fun findById(id: String): FinancialAccount?

    suspend fun save(account: FinancialAccount)
}
