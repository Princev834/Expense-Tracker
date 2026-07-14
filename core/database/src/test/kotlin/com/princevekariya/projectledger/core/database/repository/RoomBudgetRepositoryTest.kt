package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.BudgetDao
import com.princevekariya.projectledger.core.database.entity.BudgetEntity
import com.princevekariya.projectledger.core.model.Budget
import com.princevekariya.projectledger.core.model.BudgetPeriod
import com.princevekariya.projectledger.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomBudgetRepositoryTest {
    @Test
    fun observeEnabledMapsBudgetModels() = runBlocking {
        val dao = FakeBudgetDao()
        val repository = RoomBudgetRepository(budgetDao = dao)
        val budget = Budget(
            id = "monthly-food",
            name = "Monthly food",
            limit = Money(minorUnits = 500_000L),
            period = BudgetPeriod.MONTHLY,
            startEpochMillis = 1_000L,
            endEpochMillis = 2_000L,
            categoryId = "food",
        )

        repository.save(budget = budget)

        assertEquals(listOf(budget), repository.observeEnabled().first())
        assertEquals(budget, repository.findById(id = budget.id))
    }

    private class FakeBudgetDao : BudgetDao {
        private val budgets = linkedMapOf<String, BudgetEntity>()
        private val state = MutableStateFlow<List<BudgetEntity>>(emptyList())

        override suspend fun upsert(budget: BudgetEntity) {
            budgets[budget.id] = budget
            state.value = budgets.values.toList()
        }

        override fun observeEnabled(): Flow<List<BudgetEntity>> = state.map { entities ->
            entities.filter { entity -> entity.isEnabled }
        }

        override suspend fun findById(id: String): BudgetEntity? = budgets[id]

        override suspend fun count(): Int = budgets.size
    }
}
