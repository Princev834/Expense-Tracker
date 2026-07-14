package com.princevekariya.projectledger.domain.transactions.repository

import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeAll(): Flow<List<LedgerTransaction>>

    fun observeRecent(limit: Int): Flow<List<LedgerTransaction>>

    suspend fun findById(id: String): LedgerTransaction?

    suspend fun save(transaction: LedgerTransaction)

    suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount)

    suspend fun deleteById(id: String): Boolean
}
