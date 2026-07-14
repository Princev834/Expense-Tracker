package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.TransactionDao
import com.princevekariya.projectledger.core.database.entity.TransactionEntity
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTransactionRepositoryTest {
    @Test
    fun saveObserveFindAndDeleteMapTransactionModels() = runBlocking {
        val dao = FakeTransactionDao()
        val repository = RoomTransactionRepository(transactionDao = dao)
        val transaction = expenseTransaction(
            id = "transaction-one",
            occurredAtEpochMillis = 2_000L,
        )

        repository.save(transaction = transaction)

        assertEquals(listOf(transaction), repository.observeAll().first())
        assertEquals(transaction, repository.findById(id = transaction.id))
        assertTrue(repository.deleteById(id = transaction.id))
        assertFalse(repository.deleteById(id = transaction.id))
    }

    @Test
    fun recentTransactionsRespectTheRequestedLimit() = runBlocking {
        val repository = RoomTransactionRepository(
            transactionDao = FakeTransactionDao(),
        )
        val older = expenseTransaction(
            id = "older",
            occurredAtEpochMillis = 1_000L,
        )
        val newer = expenseTransaction(
            id = "newer",
            occurredAtEpochMillis = 2_000L,
        )

        repository.save(transaction = older)
        repository.save(transaction = newer)

        assertEquals(
            listOf(newer),
            repository.observeRecent(limit = 1).first(),
        )
    }

    @Test
    fun recentTransactionsRejectNonPositiveLimits() {
        val repository = RoomTransactionRepository(
            transactionDao = FakeTransactionDao(),
        )

        val failure = runCatching {
            repository.observeRecent(limit = 0)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun expenseTransaction(id: String, occurredAtEpochMillis: Long): LedgerTransaction = LedgerTransaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = Money(minorUnits = 12_000L),
        accountId = "cash",
        categoryId = "food",
        occurredAtEpochMillis = occurredAtEpochMillis,
        paymentMethod = PaymentMethod.CASH,
    )

    private class FakeTransactionDao : TransactionDao {
        private val transactions = linkedMapOf<String, TransactionEntity>()
        private val state = MutableStateFlow<List<TransactionEntity>>(emptyList())

        override suspend fun upsert(transaction: TransactionEntity) {
            transactions[transaction.id] = transaction
            publish()
        }

        override fun observeAll(): Flow<List<TransactionEntity>> = state

        override fun observeRecent(limit: Int): Flow<List<TransactionEntity>> =
            state.map { entities -> entities.take(limit) }

        override suspend fun findById(id: String): TransactionEntity? = transactions[id]

        override suspend fun deleteById(id: String): Int {
            val wasRemoved = transactions.remove(id) != null
            publish()
            return if (wasRemoved) 1 else 0
        }

        override suspend fun count(): Int = transactions.size

        private fun publish() {
            state.value = transactions.values.sortedWith(
                compareByDescending<TransactionEntity> { entity ->
                    entity.occurredAtEpochMillis
                }.thenByDescending { entity ->
                    entity.createdAtEpochMillis
                },
            )
        }
    }
}
