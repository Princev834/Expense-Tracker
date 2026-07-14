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
import com.princevekariya.projectledger.platform.device.AndroidAppLogger
import com.princevekariya.projectledger.platform.device.AndroidProcessErrorReporter
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
        appContainer = DefaultAppContainer(
            appLogger = appLogger,
            repositories = repositories,
            ensureDefaultLedgerData = EnsureDefaultLedgerDataUseCase(
                accountRepository = repositories.accounts,
                categoryRepository = repositories.categories,
            ),
            saveManualTransaction = SaveManualTransactionUseCase(
                accountRepository = repositories.accounts,
                categoryRepository = repositories.categories,
                merchantRepository = repositories.merchants,
                transactionRepository = repositories.transactions,
                idGenerator = UuidTransactionIdGenerator,
                timeProvider = SystemEpochTimeProvider,
            ),
        )

        appLogger.info(
            event = "application_container_ready",
            message = "Logging and five local repositories are ready.",
        )
        appLogger.info(
            event = "manual_transaction_use_case_ready",
            message = "Manual expense and income saving is ready.",
        )
        initializeDefaultLedgerData()
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
