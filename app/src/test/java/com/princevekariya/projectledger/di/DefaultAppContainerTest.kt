package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.NoOpAppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
import com.princevekariya.projectledger.core.model.AppDistribution
import com.princevekariya.projectledger.core.model.AppVariantConfiguration
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase
import com.princevekariya.projectledger.domain.transactions.command.TransactionIdGenerator
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.BudgetRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository
import com.princevekariya.projectledger.domain.transactions.repository.MerchantRepository
import com.princevekariya.projectledger.domain.transactions.repository.TransactionRepository
import com.princevekariya.projectledger.feature.dashboard.DashboardRepositories
import com.princevekariya.projectledger.feature.dashboard.DashboardUiState
import com.princevekariya.projectledger.feature.dashboard.DashboardViewModelFactory
import com.princevekariya.projectledger.feature.reports.MonthlyReportRepositories
import com.princevekariya.projectledger.feature.reports.MonthlyReportViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionEntryViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryRepositories
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryViewModelFactory
import java.lang.reflect.Proxy
import java.time.ZoneOffset
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultAppContainerTest {
    @Test
    fun containerReturnsEveryDependencyProvidedByTheCompositionRoot() {
        val fixture = Fixture()

        assertSame(NoOpAppLogger, fixture.container.appLogger)
        assertSame(fixture.repositories, fixture.container.repositories)
        assertSame(
            fixture.initializer,
            fixture.container.ensureDefaultLedgerData,
        )
        assertSame(
            fixture.saver,
            fixture.container.saveManualTransaction,
        )
        assertSame(
            fixture.entryFactory,
            fixture.container.transactionEntryViewModelFactory,
        )
        assertSame(
            fixture.historyFactory,
            fixture.container.transactionHistoryViewModelFactory,
        )
        assertSame(
            fixture.reportFactory,
            fixture.container.monthlyReportViewModelFactory,
        )
        assertSame(
            fixture.dashboardFactory,
            fixture.container.createDashboardViewModelFactory(
                initialState = fixture.initialDashboardState,
            ),
        )
    }

    private class Fixture {
        private val accounts = unusedProxy<AccountRepository>()
        private val categories = unusedProxy<CategoryRepository>()
        private val merchants = unusedProxy<MerchantRepository>()
        private val transactions = unusedProxy<TransactionRepository>()

        val repositories = LedgerRepositories(
            accounts = accounts,
            budgets = unusedProxy<BudgetRepository>(),
            categories = categories,
            merchants = merchants,
            transactions = transactions,
        )
        val initializer = EnsureDefaultLedgerDataUseCase(
            accountRepository = accounts,
            categoryRepository = categories,
        )
        val saver = SaveManualTransactionUseCase(
            accountRepository = accounts,
            categoryRepository = categories,
            merchantRepository = merchants,
            transactionRepository = transactions,
            idGenerator = TransactionIdGenerator {
                "unused-id"
            },
            timeProvider = EpochTimeProvider {
                0L
            },
        )
        val entryFactory = TransactionEntryViewModelFactory(
            accountRepository = accounts,
            categoryRepository = categories,
            saveManualTransaction = saver,
            appLogger = NoOpAppLogger,
        )
        val historyFactory = TransactionHistoryViewModelFactory(
            repositories = TransactionHistoryRepositories(
                accounts = accounts,
                transactions = transactions,
                categories = categories,
                merchants = merchants,
            ),
            timeProvider = zeroTimeProvider(),
            zoneId = ZoneOffset.UTC,
            appLogger = NoOpAppLogger,
        )
        val reportFactory = MonthlyReportViewModelFactory(
            repositories = MonthlyReportRepositories(
                transactions = transactions,
                categories = categories,
            ),
            timeProvider = zeroTimeProvider(),
            zoneId = ZoneOffset.UTC,
            appLogger = NoOpAppLogger,
        )
        val initialDashboardState = dashboardState()
        val dashboardFactory = DashboardViewModelFactory(
            initialState = initialDashboardState,
            repositories = DashboardRepositories(
                accounts = accounts,
                transactions = transactions,
                categories = categories,
                merchants = merchants,
            ),
            timeProvider = zeroTimeProvider(),
            zoneId = ZoneOffset.UTC,
            appLogger = NoOpAppLogger,
        )
        val container = DefaultAppContainer(
            appLogger = NoOpAppLogger,
            repositories = repositories,
            ensureDefaultLedgerData = initializer,
            saveManualTransaction = saver,
            transactionEntryViewModelFactory = entryFactory,
            transactionHistoryViewModelFactory = historyFactory,
            monthlyReportViewModelFactory = reportFactory,
            dashboardViewModelFactoryProvider = {
                dashboardFactory
            },
        )
    }

    private companion object {
        fun dashboardState(): DashboardUiState = DashboardUiState(
            variant = AppVariantConfiguration(
                distribution = AppDistribution.PERSONAL,
                displayName = "Personal APK",
                supportsSmsAutomation = true,
                isPlayStoreSafe = false,
            ),
            platformDescription = "Android test device",
            moduleCount = 10,
        )

        fun zeroTimeProvider(): EpochTimeProvider = EpochTimeProvider {
            0L
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> unusedProxy(): T = Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ ->
            error("Unexpected repository call: ${method.name}")
        } as T
    }
}
