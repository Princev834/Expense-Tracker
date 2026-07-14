package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.MerchantDao
import com.princevekariya.projectledger.core.database.mapper.toEntity
import com.princevekariya.projectledger.core.database.mapper.toModel
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMerchantRepository(
    private val merchantDao: MerchantDao,
) : MerchantRepository {
    override fun observeActive(): Flow<List<Merchant>> = merchantDao.observeActive().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    override suspend fun findBySearchKey(searchKey: String): Merchant? {
        val normalizedSearchKey = searchKey
            .trim()
            .lowercase(Locale.ENGLISH)
        require(normalizedSearchKey.isNotBlank()) {
            "Merchant search key cannot be blank."
        }
        return merchantDao.findBySearchKey(searchKey = normalizedSearchKey)?.toModel()
    }

    override suspend fun save(merchant: Merchant) {
        merchantDao.upsert(merchant = merchant.toEntity())
    }
}
