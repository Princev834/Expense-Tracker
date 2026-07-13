package com.princevekariya.projectledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.princevekariya.projectledger.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query(
        """
        SELECT * FROM budgets
        WHERE is_enabled = 1
        ORDER BY start_epoch_millis DESC, name COLLATE NOCASE
        """,
    )
    fun observeEnabled(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BudgetEntity?

    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun count(): Int
}
