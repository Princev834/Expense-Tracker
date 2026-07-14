package com.princevekariya.projectledger.domain.transactions.repository

import com.princevekariya.projectledger.core.model.Merchant
import kotlinx.coroutines.flow.Flow

interface MerchantRepository {
    fun observeActive(): Flow<List<Merchant>>

    suspend fun findBySearchKey(searchKey: String): Merchant?

    suspend fun save(merchant: Merchant)
}
