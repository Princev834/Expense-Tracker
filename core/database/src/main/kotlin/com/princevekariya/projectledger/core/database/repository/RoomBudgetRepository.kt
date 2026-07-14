package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.BudgetDao
import com.princevekariya.projectledger.core.database.mapper.toEntity
import com.princevekariya.projectledger.core.database.mapper.toModel
import com.princevekariya.projectledger.core.model.Budget
import com.princevekariya.projectledger.domain.transactions.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomBudgetRepository(
    private val budgetDao: BudgetDao,
) : BudgetRepository {
    override fun observeEnabled(): Flow<List<Budget>> = budgetDao.observeEnabled().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    override suspend fun findById(id: String): Budget? {
        require(id.isNotBlank()) {
            "Budget identifier cannot be blank."
        }
        return budgetDao.findById(id = id)?.toModel()
    }

    override suspend fun save(budget: Budget) {
        budgetDao.upsert(budget = budget.toEntity())
    }
}
