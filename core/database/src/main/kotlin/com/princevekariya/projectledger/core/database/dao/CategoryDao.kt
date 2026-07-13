package com.princevekariya.projectledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.princevekariya.projectledger.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query(
        """
        SELECT * FROM transaction_categories
        WHERE type = :type AND is_archived = 0
        ORDER BY is_default DESC, name COLLATE NOCASE
        """,
    )
    fun observeActiveByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM transaction_categories WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM transaction_categories")
    suspend fun count(): Int
}
