package com.princevekariya.projectledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.princevekariya.projectledger.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query(
        """
        SELECT * FROM ledger_transactions
        ORDER BY occurred_at_epoch_millis DESC, created_at_epoch_millis DESC
        """,
    )
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM ledger_transactions
        ORDER BY occurred_at_epoch_millis DESC, created_at_epoch_millis DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TransactionEntity?

    @Query("DELETE FROM ledger_transactions WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT COUNT(*) FROM ledger_transactions")
    suspend fun count(): Int
}
