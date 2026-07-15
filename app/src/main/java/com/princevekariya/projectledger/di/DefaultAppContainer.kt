package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase
import com.princevekariya.projectledger.feature.dashboard.DashboardUiState
import com.princevekariya.projectledger.feature.dashboard.DashboardViewModelFactory
import com.princevekariya.projectledger.feature.reports.MonthlyReportViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionEntryViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryViewModelFactory

class DefaultAppContainer(
    override val appLogger: AppLogger,
    override val repositories: LedgerRepositories,
    override val ensureDefaultLedgerData: EnsureDefaultLedgerDataUseCase,
    override val saveManualTransaction: SaveManualTransactionUseCase,
    override val transactionEntryViewModelFactory: TransactionEntryViewModelFactory,
    override val transactionHistoryViewModelFactory: TransactionHistoryViewModelFactory,
    override val monthlyReportViewModelFactory: MonthlyReportViewModelFactory,
    private val dashboardViewModelFactoryProvider: (DashboardUiState) -> DashboardViewModelFactory,
) : AppContainer {
    override fun createDashboardViewModelFactory(initialState: DashboardUiState): DashboardViewModelFactory =
        dashboardViewModelFactoryProvider(initialState)
}
