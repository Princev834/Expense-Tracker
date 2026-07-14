package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.AccountDao
import com.princevekariya.projectledger.core.database.entity.AccountEntity
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomAccountRepositoryTest {
    @Test
    fun saveAndObserveMapsAccountModels() = runBlocking {
        val dao = FakeAccountDao()
        val repository = RoomAccountRepository(accountDao = dao)
        val account = FinancialAccount(
            id = "cash",
            name = "Cash",
            type = AccountType.CASH,
            openingBalance = Money(minorUnits = 10_000L),
        )

        repository.save(account = account)

        assertEquals(listOf(account), repository.observeAll().first())
        assertEquals(account, repository.findById(id = account.id))
        assertNull(repository.findById(id = "missing"))
    }

    private class FakeAccountDao : AccountDao {
        private val accounts = linkedMapOf<String, AccountEntity>()
        private val state = MutableStateFlow<List<AccountEntity>>(emptyList())

        override suspend fun upsert(account: AccountEntity) {
            accounts[account.id] = account
            state.value = accounts.values.sortedBy { entity -> entity.name }
        }

        override fun observeAll(): Flow<List<AccountEntity>> = state

        override suspend fun findById(id: String): AccountEntity? = accounts[id]

        override suspend fun count(): Int = accounts.size
    }
}
