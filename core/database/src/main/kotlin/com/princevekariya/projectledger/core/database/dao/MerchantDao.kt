package com.princevekariya.projectledger.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.princevekariya.projectledger.core.database.entity.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    @Upsert
    suspend fun upsert(merchant: MerchantEntity)

    @Query(
        """
        SELECT * FROM merchants
        WHERE is_archived = 0
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeActive(): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE search_key = :searchKey LIMIT 1")
    suspend fun findBySearchKey(searchKey: String): MerchantEntity?

    @Query("SELECT COUNT(*) FROM merchants")
    suspend fun count(): Int
}
