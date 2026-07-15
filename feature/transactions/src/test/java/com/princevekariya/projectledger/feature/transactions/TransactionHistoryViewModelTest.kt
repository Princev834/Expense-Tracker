package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.common.AppLogLevel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Merchant
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun liveDataBuildsSortedTransactionHistory() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UiLoadState.Content, state.loadState)
        assertEquals(3, state.totalTransactionCount)
        assertEquals(
            listOf(
                "transaction-expense",
                "transaction-income",
                "transaction-old",
            ),
            state.transactions.map { item -> item.id },
        )
        assertEquals("Swiggy", state.transactions[0].title)
        assertEquals(
            "Food and Dining - Cash - Today - UPI",
            state.transactions[0].subtitle,
        )
        assertEquals("July stipend", state.transactions[1].title)
        assertEquals(
            "Salary - Cash - Yesterday - Bank transfer",
            state.transactions[1].subtitle,
        )
    }

    @Test
    fun expenseAndIncomeFiltersUpdateVisibleRows() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(
            TransactionHistoryAction.FilterSelected(
                filter = TransactionHistoryFilter.INCOME,
            ),
        )

        assertEquals(
            TransactionHistoryFilter.INCOME,
            viewModel.uiState.value.selectedFilter,
        )
        assertEquals(
            listOf("transaction-income"),
            viewModel.uiState.value.transactions.map { item -> item.id },
        )

        viewModel.onAction(
            TransactionHistoryAction.FilterSelected(
                filter = TransactionHistoryFilter.EXPENSE,
            ),
        )

        assertEquals(
            listOf(
                "transaction-expense",
                "transaction-old",
            ),
            viewModel.uiState.value.transactions.map { item -> item.id },
        )
    }

    @Test
    fun repositoryChangesRefreshTheVisibleHistory() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        fixture.transactions.emit(
            fixture.incomeTransaction(
                id = "transaction-new-income",
                occurredAt = TODAY,
                note = "Refund received",
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.totalTransactionCount)
        assertEquals(
            "transaction-new-income",
            state.transactions.single().id,
        )
        assertEquals(
            "Refund received",
            state.transactions.single().title,
        )
    }

    @Test
    fun sourceFailureProducesErrorStateAndSafeLog() = runTest {
        val logger = RecordingAppLogger()
        val fixture = Fixture()
        val viewModel = TransactionHistoryViewModel(
            repositories = TransactionHistoryRepositories(
                accounts = FailingAccountRepository(),
                transactions = fixture.transactions,
                categories = fixture.categories,
                merchants = fixture.merchants,
            ),
            dataMapper = mapper(),
            appLogger = logger,
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadState is UiLoadState.Error)
        assertEquals(AppLogLevel.ERROR, logger.entries.single().level)
        assertEquals(
            "transaction_history_load_failed",
            logger.entries.single().event,
        )
    }

    private class Fixture {
        val accounts = FakeAccountRepository(
            initialAccounts = listOf(
                FinancialAccount(
                    id = "account-cash",
                    name = "Cash",
                    type = AccountType.CASH,
                ),
            ),
        )
        val transactions = FakeTransactionRepository(
            initialTransactions = listOf(
                expenseTransaction(
                    id = "transaction-expense",
                    occurredAt = TODAY,
                    merchantId = "merchant-swiggy",
                ),
                incomeTransaction(
                    id = "transaction-income",
                    occurredAt = YESTERDAY,
                    note = "July stipend",
                ),
                expenseTransaction(
                    id = "transaction-old",
                    occurredAt = OLDER_DATE,
                ),
            ),
        )
        val categories = FakeCategoryRepository(
            initialCategories = listOf(
                TransactionCategory(
                    id = "category-food",
                    name = "Food and Dining",
                    type = CategoryType.EXPENSE,
                    iconKey = "restaurant",
                ),
                TransactionCategory(
                    id = "category-salary",
                    name = "Salary",
                    type = CategoryType.INCOME,
                    iconKey = "payments",
                ),
            ),
        )
        val merchants = FakeMerchantRepository(
            initialMerchants = listOf(
                Merchant(
                    id = "merchant-swiggy",
                    name = "Swiggy",
                ),
            ),
        )

        fun createViewModel(): TransactionHistoryViewModel = TransactionHistoryViewModel(
            repositories = TransactionHistoryRepositories(
                accounts = accounts,
                transactions = transactions,
                categories = categories,
                merchants = merchants,
            ),
            dataMapper = mapper(),
            appLogger = RecordingAppLogger(),
        )

        fun incomeTransaction(id: String, occurredAt: Long, note: String): LedgerTransaction = LedgerTransaction(
            id = id,
            type = TransactionType.INCOME,
            amount = Money(minorUnits = 20_000L),
            accountId = "account-cash",
            categoryId = "category-salary",
            occurredAtEpochMillis = occurredAt,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            note = note,
        )

        private fun expenseTransaction(id: String, occurredAt: Long, merchantId: String? = null): LedgerTransaction =
            LedgerTransaction(
                id = id,
                type = TransactionType.EXPENSE,
                amount = Money(minorUnits = 12_000L),
                accountId = "account-cash",
                categoryId = "category-food",
                merchantId = merchantId,
                occurredAtEpochMillis = occurredAt,
                paymentMethod = PaymentMethod.UPI,
            )
    }

    private class FakeAccountRepository(
        initialAccounts: List<FinancialAccount>,
    ) : AccountRepository {
        private val state = MutableStateFlow(initialAccounts)

        override fun observeAll(): Flow<List<FinancialAccount>> = state

        override suspend fun findById(id: String): FinancialAccount? = state.value.firstOrNull { account ->
            account.id == id
        }

        override suspend fun save(account: FinancialAccount) {
            state.value = listOf(account)
        }
    }

    private class FailingAccountRepository : AccountRepository {
        override fun observeAll(): Flow<List<FinancialAccount>> = flow {
            throw IllegalStateException("Database unavailable")
        }

        override suspend fun findById(id: String): FinancialAccount? = null

        override suspend fun save(account: FinancialAccount) = Unit
    }

    private class FakeCategoryRepository(
        initialCategories: List<TransactionCategory>,
    ) : CategoryRepository {
        private val state = MutableStateFlow(initialCategories)

        override fun observeActive(type: CategoryType): Flow<List<TransactionCategory>> = state.map { categories ->
            categories.filter { category ->
                category.type == type && !category.isArchived
            }
        }

        override suspend fun findById(id: String): TransactionCategory? = state.value.firstOrNull { category ->
            category.id == id
        }

        override suspend fun save(category: TransactionCategory) = Unit
    }

    private class FakeMerchantRepository(
        initialMerchants: List<Merchant>,
    ) : MerchantRepository {
        private val state = MutableStateFlow(initialMerchants)

        override fun observeActive(): Flow<List<Merchant>> = state

        override suspend fun findBySearchKey(searchKey: String): Merchant? = state.value.firstOrNull { merchant ->
            merchant.searchKey == searchKey
        }

        override suspend fun save(merchant: Merchant) = Unit
    }

    private class FakeTransactionRepository(
        initialTransactions: List<LedgerTransaction>,
    ) : TransactionRepository {
        private val state = MutableStateFlow(initialTransactions)

        override fun observeAll(): Flow<List<LedgerTransaction>> = state

        override fun observeRecent(limit: Int): Flow<List<LedgerTransaction>> = state.map { transactions ->
            transactions.take(limit)
        }

        override suspend fun findById(id: String): LedgerTransaction? = state.value.firstOrNull { transaction ->
            transaction.id == id
        }

        override suspend fun save(transaction: LedgerTransaction) {
            emit(transaction = transaction)
        }

        override suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount) {
            emit(transaction = transaction)
        }

        override suspend fun deleteById(id: String): Boolean {
            val oldSize = state.value.size
            state.value = state.value.filterNot { transaction ->
                transaction.id == id
            }
            return state.value.size < oldSize
        }

        fun emit(transaction: LedgerTransaction) {
            state.value = listOf(transaction)
        }
    }

    private class RecordingAppLogger : AppLogger {
        val entries = mutableListOf<LogEntry>()

        override fun log(level: AppLogLevel, event: String, message: String, throwable: Throwable?) {
            entries += LogEntry(
                level = level,
                event = event,
            )
        }
    }

    private data class LogEntry(
        val level: AppLogLevel,
        val event: String,
    )

    private companion object {
        val NOW: Long = Instant.parse(
            "2026-07-15T12:00:00Z",
        ).toEpochMilli()
        val TODAY: Long = Instant.parse(
            "2026-07-15T08:00:00Z",
        ).toEpochMilli()
        val YESTERDAY: Long = Instant.parse(
            "2026-07-14T08:00:00Z",
        ).toEpochMilli()
        val OLDER_DATE: Long = Instant.parse(
            "2026-06-30T08:00:00Z",
        ).toEpochMilli()

        fun mapper(): TransactionHistoryDataMapper = TransactionHistoryDataMapper(
            timeProvider = EpochTimeProvider {
                NOW
            },
            zoneId = ZoneOffset.UTC,
        )
    }
}
