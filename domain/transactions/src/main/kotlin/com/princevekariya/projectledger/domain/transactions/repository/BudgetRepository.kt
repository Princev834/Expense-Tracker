package com.princevekariya.projectledger.domain.transactions.repository

import com.princevekariya.projectledger.core.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeEnabled(): Flow<List<Budget>>

    suspend fun findById(id: String): Budget?

    suspend fun save(budget: Budget)
}
