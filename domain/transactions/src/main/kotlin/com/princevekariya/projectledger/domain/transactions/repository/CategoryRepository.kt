package com.princevekariya.projectledger.domain.transactions.repository

import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActive(type: CategoryType): Flow<List<TransactionCategory>>

    suspend fun findById(id: String): TransactionCategory?

    suspend fun save(category: TransactionCategory)
}
