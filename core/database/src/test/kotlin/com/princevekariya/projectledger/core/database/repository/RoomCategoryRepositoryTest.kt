package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.CategoryDao
import com.princevekariya.projectledger.core.database.entity.CategoryEntity
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomCategoryRepositoryTest {
    @Test
    fun observeActiveMapsOnlyRequestedCategoryType() = runBlocking {
        val dao = FakeCategoryDao()
        val repository = RoomCategoryRepository(categoryDao = dao)
        val expense = TransactionCategory(
            id = "food",
            name = "Food",
            type = CategoryType.EXPENSE,
            iconKey = "restaurant",
        )
        val income = TransactionCategory(
            id = "salary",
            name = "Salary",
            type = CategoryType.INCOME,
            iconKey = "payments",
        )

        repository.save(category = expense)
        repository.save(category = income)

        assertEquals(
            listOf(expense),
            repository.observeActive(type = CategoryType.EXPENSE).first(),
        )
        assertEquals(income, repository.findById(id = income.id))
    }

    private class FakeCategoryDao : CategoryDao {
        private val categories = linkedMapOf<String, CategoryEntity>()
        private val state = MutableStateFlow<List<CategoryEntity>>(emptyList())

        override suspend fun upsert(category: CategoryEntity) {
            categories[category.id] = category
            state.value = categories.values.toList()
        }

        override fun observeActiveByType(type: String): Flow<List<CategoryEntity>> = state.map { entities ->
            entities.filter { entity ->
                entity.type == type && !entity.isArchived
            }
        }

        override suspend fun findById(id: String): CategoryEntity? = categories[id]

        override suspend fun count(): Int = categories.size
    }
}
