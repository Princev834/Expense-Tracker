package com.princevekariya.projectledger.core.database.repository

import com.princevekariya.projectledger.core.database.dao.AccountDao
import com.princevekariya.projectledger.core.database.dao.TransactionDao
import com.princevekariya.projectledger.core.database.entity.AccountEntity
import com.princevekariya.projectledger.core.database.entity.TransactionEntity
import com.princevekariya.projectledger.core.database.transaction.DatabaseTransactionRunner
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.FinancialAccount
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
        val fixture = Fixture()
        val transaction = expenseTransaction(
            id = "transaction-one",
            occurredAtEpochMillis = 2_000L,
        )

        fixture.repository.save(transaction = transaction)

        assertEquals(
            listOf(transaction),
            fixture.repository.observeAll().first(),
        )
        assertEquals(
            transaction,
            fixture.repository.findById(id = transaction.id),
        )
        assertTrue(
            fixture.repository.deleteById(id = transaction.id),
        )
        assertFalse(
            fixture.repository.deleteById(id = transaction.id),
        )
    }

    @Test
    fun recentTransactionsRespectTheRequestedLimit() = runBlocking {
        val fixture = Fixture()
        val older = expenseTransaction(
            id = "older",
            occurredAtEpochMillis = 1_000L,
        )
        val newer = expenseTransaction(
            id = "newer",
            occurredAtEpochMillis = 2_000L,
        )

        fixture.repository.save(transaction = older)
        fixture.repository.save(transaction = newer)

        assertEquals(
            listOf(newer),
            fixture.repository.observeRecent(limit = 1).first(),
        )
    }

    @Test
    fun balanceAwareSaveWritesAccountAndTransactionInOneRunner() = runBlocking {
        val fixture = Fixture()
        val transaction = expenseTransaction(
            id = "balanced",
            occurredAtEpochMillis = 3_000L,
        )
        val account = FinancialAccount(
            id = "cash",
            name = "Cash",
            type = AccountType.CASH,
            openingBalance = Money(minorUnits = 50_000L),
            currentBalance = Money(minorUnits = 38_000L),
        )

        fixture.repository.saveWithUpdatedAccount(
            transaction = transaction,
            updatedAccount = account,
        )

        assertEquals(1, fixture.transactionRunner.runCount)
        assertEquals(
            account.currentBalance.minorUnits,
            fixture.accountDao
                .findById(id = account.id)
                ?.currentBalanceMinorUnits,
        )
        assertEquals(
            transaction.id,
            fixture.transactionDao
                .findById(id = transaction.id)
                ?.id,
        )
    }

    @Test
    fun balanceAwareSaveRejectsMismatchedAccount() = runBlocking {
        val fixture = Fixture()
        val failure = runCatching {
            fixture.repository.saveWithUpdatedAccount(
                transaction = expenseTransaction(
                    id = "mismatch",
                    occurredAtEpochMillis = 4_000L,
                ),
                updatedAccount = FinancialAccount(
                    id = "bank",
                    name = "Bank",
                    type = AccountType.BANK_ACCOUNT,
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(0, fixture.transactionRunner.runCount)
    }

    @Test
    fun recentTransactionsRejectNonPositiveLimits() {
        val fixture = Fixture()

        val failure = runCatching {
            fixture.repository.observeRecent(limit = 0)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private class Fixture {
        val transactionDao = FakeTransactionDao()
        val accountDao = FakeAccountDao()
        val transactionRunner = RecordingTransactionRunner()
        val repository = RoomTransactionRepository(
            transactionDao = transactionDao,
            accountDao = accountDao,
            transactionRunner = transactionRunner,
        )
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

    private class RecordingTransactionRunner :
        DatabaseTransactionRunner {
        var runCount: Int = 0
            private set

        override suspend fun <T> run(block: suspend () -> T): T {
            runCount += 1
            return block()
        }
    }

    private class FakeAccountDao : AccountDao {
        private val accounts =
            linkedMapOf<String, AccountEntity>()
        private val state =
            MutableStateFlow<List<AccountEntity>>(emptyList())

        override suspend fun upsert(account: AccountEntity) {
            accounts[account.id] = account
            state.value = accounts.values.toList()
        }

        override fun observeAll(): Flow<List<AccountEntity>> = state

        override suspend fun findById(id: String): AccountEntity? = accounts[id]

        override suspend fun count(): Int = accounts.size
    }

    private class FakeTransactionDao : TransactionDao {
        private val transactions =
            linkedMapOf<String, TransactionEntity>()
        private val state =
            MutableStateFlow<List<TransactionEntity>>(emptyList())

        override suspend fun upsert(transaction: TransactionEntity) {
            transactions[transaction.id] = transaction
            publish()
        }

        override fun observeAll(): Flow<List<TransactionEntity>> = state

        override fun observeRecent(limit: Int): Flow<List<TransactionEntity>> = state.map { entities ->
            entities.take(limit)
        }

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
