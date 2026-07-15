package com.princevekariya.projectledger.feature.reports

import com.princevekariya.projectledger.core.common.AppLogLevel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.CategoryType
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun currentMonthBuildsExactSummaryAndCategoryBreakdown() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UiLoadState.Content, state.loadState)
        assertEquals(YearMonth.of(2026, 7), state.selectedMonth)
        assertEquals("July 2026", state.selectedMonthLabel)
        assertEquals(100_000L, state.income.minorUnits)
        assertEquals(35_000L, state.expenses.minorUnits)
        assertEquals(65_000L, state.netCashFlow.minorUnits)
        assertEquals(4, state.transactionCount)
        assertEquals(
            listOf("category-food", "category-travel"),
            state.categoryExpenses.map { category -> category.id },
        )
        assertEquals(71, state.categoryExpenses[0].sharePercent)
        assertEquals(2, state.categoryExpenses[0].transactionCount)
        assertEquals(29, state.categoryExpenses[1].sharePercent)
        assertFalse(state.canMoveNext)
    }

    @Test
    fun monthNavigationMovesBackwardAndNeverMovesIntoTheFuture() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        viewModel.onAction(
            MonthlyReportAction.PreviousMonthRequested,
        )

        val juneState = viewModel.uiState.value
        assertEquals(YearMonth.of(2026, 6), juneState.selectedMonth)
        assertEquals("June 2026", juneState.selectedMonthLabel)
        assertEquals(5_000L, juneState.expenses.minorUnits)
        assertEquals(1, juneState.transactionCount)
        assertTrue(juneState.canMoveNext)

        viewModel.onAction(MonthlyReportAction.NextMonthRequested)
        viewModel.onAction(MonthlyReportAction.NextMonthRequested)

        assertEquals(
            YearMonth.of(2026, 7),
            viewModel.uiState.value.selectedMonth,
        )
    }

    @Test
    fun repositoryChangesRefreshTheSelectedMonth() = runTest {
        val fixture = Fixture()
        val viewModel = fixture.createViewModel()
        advanceUntilIdle()

        fixture.transactions.emit(
            transactions = listOf(
                fixture.transaction(
                    id = "new-expense",
                    type = TransactionType.EXPENSE,
                    amountMinorUnits = 20_000L,
                    categoryId = "category-food",
                    occurredAt = JULY_DATE,
                ),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(20_000L, state.expenses.minorUnits)
        assertEquals(0L, state.income.minorUnits)
        assertEquals(-20_000L, state.netCashFlow.minorUnits)
        assertEquals(1, state.transactionCount)
        assertEquals(1, state.categoryExpenses.single().transactionCount)
    }

    @Test
    fun sourceFailureProducesErrorStateAndSafeLog() = runTest {
        val logger = RecordingAppLogger()
        val fixture = Fixture()
        val viewModel = MonthlyReportViewModel(
            repositories = MonthlyReportRepositories(
                transactions = FailingTransactionRepository(),
                categories = fixture.categories,
            ),
            dataMapper = mapper(),
            appLogger = logger,
        )

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.loadState is UiLoadState.Error)
        assertEquals(AppLogLevel.ERROR, logger.entries.single().level)
        assertEquals(
            "monthly_report_load_failed",
            logger.entries.single().event,
        )
    }

    private class Fixture {
        val categories = FakeCategoryRepository(
            initialCategories = listOf(
                TransactionCategory(
                    id = "category-food",
                    name = "Food and Dining",
                    type = CategoryType.EXPENSE,
                    iconKey = "restaurant",
                ),
                TransactionCategory(
                    id = "category-travel",
                    name = "Travel",
                    type = CategoryType.EXPENSE,
                    iconKey = "travel",
                ),
            ),
        )
        val transactions = FakeTransactionRepository(
            initialTransactions = listOf(
                transaction(
                    id = "income",
                    type = TransactionType.INCOME,
                    amountMinorUnits = 100_000L,
                    categoryId = "category-salary",
                    occurredAt = JULY_DATE,
                ),
                transaction(
                    id = "food-one",
                    type = TransactionType.EXPENSE,
                    amountMinorUnits = 15_000L,
                    categoryId = "category-food",
                    occurredAt = JULY_DATE,
                ),
                transaction(
                    id = "food-two",
                    type = TransactionType.EXPENSE,
                    amountMinorUnits = 10_000L,
                    categoryId = "category-food",
                    occurredAt = JULY_DATE,
                ),
                transaction(
                    id = "travel",
                    type = TransactionType.EXPENSE,
                    amountMinorUnits = 10_000L,
                    categoryId = "category-travel",
                    occurredAt = JULY_DATE,
                ),
                transaction(
                    id = "june-expense",
                    type = TransactionType.EXPENSE,
                    amountMinorUnits = 5_000L,
                    categoryId = "category-food",
                    occurredAt = JUNE_DATE,
                ),
            ),
        )

        fun createViewModel(): MonthlyReportViewModel = MonthlyReportViewModel(
            repositories = MonthlyReportRepositories(
                transactions = transactions,
                categories = categories,
            ),
            dataMapper = mapper(),
            appLogger = RecordingAppLogger(),
        )

        fun transaction(
            id: String,
            type: TransactionType,
            amountMinorUnits: Long,
            categoryId: String,
            occurredAt: Long,
        ): LedgerTransaction = LedgerTransaction(
            id = id,
            type = type,
            amount = Money(minorUnits = amountMinorUnits),
            accountId = "account-cash",
            categoryId = categoryId,
            occurredAtEpochMillis = occurredAt,
            paymentMethod = PaymentMethod.CASH,
        )
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
            state.value = state.value + transaction
        }

        override suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount) {
            state.value = state.value + transaction
        }

        override suspend fun deleteById(id: String): Boolean {
            val oldSize = state.value.size
            state.value = state.value.filterNot { transaction ->
                transaction.id == id
            }
            return state.value.size < oldSize
        }

        fun emit(transactions: List<LedgerTransaction>) {
            state.value = transactions
        }
    }

    private class FailingTransactionRepository :
        TransactionRepository {
        override fun observeAll(): Flow<List<LedgerTransaction>> = flow {
            throw IllegalStateException("Database unavailable")
        }

        override fun observeRecent(limit: Int): Flow<List<LedgerTransaction>> = flow {
            throw IllegalStateException("Database unavailable")
        }

        override suspend fun findById(id: String): LedgerTransaction? = null

        override suspend fun save(transaction: LedgerTransaction) = Unit

        override suspend fun saveWithUpdatedAccount(transaction: LedgerTransaction, updatedAccount: FinancialAccount) =
            Unit

        override suspend fun deleteById(id: String): Boolean = false
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
        val JULY_DATE: Long = Instant.parse(
            "2026-07-10T08:00:00Z",
        ).toEpochMilli()
        val JUNE_DATE: Long = Instant.parse(
            "2026-06-20T08:00:00Z",
        ).toEpochMilli()

        fun mapper(): MonthlyReportDataMapper = MonthlyReportDataMapper(
            timeProvider = EpochTimeProvider {
                NOW
            },
            zoneId = ZoneOffset.UTC,
        )
    }
}
