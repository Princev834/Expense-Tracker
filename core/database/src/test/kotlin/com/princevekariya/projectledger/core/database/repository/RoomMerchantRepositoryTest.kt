package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.MerchantDao
import com.princevekariya.projectledger.core.database.entity.MerchantEntity
import com.princevekariya.projectledger.core.model.Merchant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMerchantRepositoryTest {
    @Test
    fun searchKeyLookupIsNormalized() = runBlocking {
        val dao = FakeMerchantDao()
        val repository = RoomMerchantRepository(merchantDao = dao)
        val merchant = Merchant(
            id = "merchant-swiggy",
            name = "Swiggy",
        )

        repository.save(merchant = merchant)

        assertEquals(merchant, repository.findBySearchKey(searchKey = "  SWIGGY "))
        assertEquals(listOf(merchant), repository.observeActive().first())
    }

    private class FakeMerchantDao : MerchantDao {
        private val merchants = linkedMapOf<String, MerchantEntity>()
        private val state = MutableStateFlow<List<MerchantEntity>>(emptyList())

        override suspend fun upsert(merchant: MerchantEntity) {
            merchants[merchant.id] = merchant
            state.value = merchants.values.toList()
        }

        override fun observeActive(): Flow<List<MerchantEntity>> = state.map { entities ->
            entities.filterNot { entity -> entity.isArchived }
        }

        override suspend fun findBySearchKey(searchKey: String): MerchantEntity? =
            merchants.values.firstOrNull { entity ->
                entity.searchKey == searchKey
            }

        override suspend fun count(): Int = merchants.size
    }
}
