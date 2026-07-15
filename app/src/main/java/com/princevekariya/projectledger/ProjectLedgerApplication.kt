package com.princevekariya.projectledger

import android.app.Application
import com.princevekariya.projectledger.core.common.error
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.database.ProjectLedgerDatabase
import com.princevekariya.projectledger.core.database.repository.createRepositories
import com.princevekariya.projectledger.di.AppContainer
import com.princevekariya.projectledger.di.DefaultAppContainer
import com.princevekariya.projectledger.di.SystemEpochTimeProvider
import com.princevekariya.projectledger.di.UuidTransactionIdGenerator
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase
import com.princevekariya.projectledger.feature.dashboard.DashboardRepositories
import com.princevekariya.projectledger.feature.dashboard.DashboardViewModelFactory
import com.princevekariya.projectledger.feature.reports.MonthlyReportRepositories
import com.princevekariya.projectledger.feature.reports.MonthlyReportViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionEntryViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryRepositories
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryViewModelFactory
import com.princevekariya.projectledger.platform.device.AndroidAppLogger
import com.princevekariya.projectledger.platform.device.AndroidProcessErrorReporter
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProjectLedgerApplication : Application() {
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO,
    )

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        val appLogger = AndroidAppLogger(
            verboseLoggingEnabled = BuildConfig.ENABLE_VERBOSE_LOGGING,
            includeThrowableDetails = BuildConfig.DEBUG,
        )
        AndroidProcessErrorReporter(logger = appLogger).install()

        val database = ProjectLedgerDatabase.create(context = this)
        val repositories = database.createRepositories()
        val saveManualTransaction = SaveManualTransactionUseCase(
            accountRepository = repositories.accounts,
            categoryRepository = repositories.categories,
            merchantRepository = repositories.merchants,
            transactionRepository = repositories.transactions,
            idGenerator = UuidTransactionIdGenerator,
            timeProvider = SystemEpochTimeProvider,
        )
        appContainer = DefaultAppContainer(
            appLogger = appLogger,
            repositories = repositories,
            ensureDefaultLedgerData = EnsureDefaultLedgerDataUseCase(
                accountRepository = repositories.accounts,
                categoryRepository = repositories.categories,
            ),
            saveManualTransaction = saveManualTransaction,
            transactionEntryViewModelFactory = TransactionEntryViewModelFactory(
                accountRepository = repositories.accounts,
                categoryRepository = repositories.categories,
                saveManualTransaction = saveManualTransaction,
                appLogger = appLogger,
            ),
            transactionHistoryViewModelFactory =
            TransactionHistoryViewModelFactory(
                repositories = TransactionHistoryRepositories(
                    accounts = repositories.accounts,
                    transactions = repositories.transactions,
                    categories = repositories.categories,
                    merchants = repositories.merchants,
                ),
                timeProvider = SystemEpochTimeProvider,
                zoneId = ZoneId.systemDefault(),
                appLogger = appLogger,
            ),
            monthlyReportViewModelFactory =
            MonthlyReportViewModelFactory(
                repositories = MonthlyReportRepositories(
                    transactions = repositories.transactions,
                    categories = repositories.categories,
                ),
                timeProvider = SystemEpochTimeProvider,
                zoneId = ZoneId.systemDefault(),
                appLogger = appLogger,
            ),
            dashboardViewModelFactoryProvider = { initialState ->
                DashboardViewModelFactory(
                    initialState = initialState,
                    repositories = DashboardRepositories(
                        accounts = repositories.accounts,
                        transactions = repositories.transactions,
                        categories = repositories.categories,
                        merchants = repositories.merchants,
                    ),
                    timeProvider = SystemEpochTimeProvider,
                    zoneId = ZoneId.systemDefault(),
                    appLogger = appLogger,
                )
            },
        )

        logReadyDependencies(appLogger = appLogger)
        initializeDefaultLedgerData()
    }

    private fun logReadyDependencies(appLogger: AndroidAppLogger) {
        appLogger.info(
            event = "application_container_ready",
            message = "Logging and five local repositories are ready.",
        )
        appLogger.info(
            event = "manual_transaction_use_case_ready",
            message = "Manual expense and income saving is ready.",
        )
        appLogger.info(
            event = "balance_aware_transaction_writer_ready",
            message = "Transactions and account balances will be stored atomically.",
        )
        appLogger.info(
            event = "transaction_entry_state_ready",
            message = "Transaction entry state and ViewModel factory are ready.",
        )
        appLogger.info(
            event = "live_dashboard_factory_ready",
            message = "The live Room dashboard factory is ready.",
        )
        appLogger.info(
            event = "transaction_history_factory_ready",
            message = "The live Room transaction history factory is ready.",
        )
        appLogger.info(
            event = "monthly_report_factory_ready",
            message = "The live Room monthly report factory is ready.",
        )
    }

    private fun initializeDefaultLedgerData() {
        val appLogger = appContainer.appLogger
        val initializer = appContainer.ensureDefaultLedgerData

        applicationScope.launch {
            runCatching {
                initializer()
            }.onSuccess { result ->
                appLogger.info(
                    event = "default_ledger_data_ready",
                    message = "Created ${result.createdItems} missing default records.",
                )
            }.onFailure { throwable ->
                appLogger.error(
                    event = "default_ledger_data_failed",
                    message = "Unable to prepare default ledger records.",
                    throwable = throwable,
                )
            }
        }
    }
}
