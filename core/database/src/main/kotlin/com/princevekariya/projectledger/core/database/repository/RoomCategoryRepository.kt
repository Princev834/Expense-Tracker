package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.CategoryDao
import com.princevekariya.projectledger.core.database.mapper.toEntity
import com.princevekariya.projectledger.core.database.mapper.toModel
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(
    private val categoryDao: CategoryDao,
) : CategoryRepository {
    override fun observeActive(type: CategoryType): Flow<List<TransactionCategory>> =
        categoryDao.observeActiveByType(type = type.name).map { entities ->
            entities.map { entity -> entity.toModel() }
        }

    override suspend fun findById(id: String): TransactionCategory? {
        require(id.isNotBlank()) {
            "Category identifier cannot be blank."
        }
        return categoryDao.findById(id = id)?.toModel()
    }

    override suspend fun save(category: TransactionCategory) {
        categoryDao.upsert(category = category.toEntity())
    }
}
