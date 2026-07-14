package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.AccountDao
import com.princevekariya.projectledger.core.database.mapper.toEntity
import com.princevekariya.projectledger.core.database.mapper.toModel
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAccountRepository(
    private val accountDao: AccountDao,
) : AccountRepository {
    override fun observeAll(): Flow<List<FinancialAccount>> = accountDao.observeAll().map { entities ->
        entities.map { entity -> entity.toModel() }
    }

    override suspend fun findById(id: String): FinancialAccount? {
        require(id.isNotBlank()) {
            "Account identifier cannot be blank."
        }
        return accountDao.findById(id = id)?.toModel()
    }

    override suspend fun save(account: FinancialAccount) {
        accountDao.upsert(account = account.toEntity())
    }
}
