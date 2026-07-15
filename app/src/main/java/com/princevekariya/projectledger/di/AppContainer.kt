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

interface AppContainer {
    val appLogger: AppLogger

    val repositories: LedgerRepositories

    val ensureDefaultLedgerData: EnsureDefaultLedgerDataUseCase

    val saveManualTransaction: SaveManualTransactionUseCase

    val transactionEntryViewModelFactory: TransactionEntryViewModelFactory

    val transactionHistoryViewModelFactory:
        TransactionHistoryViewModelFactory

    val monthlyReportViewModelFactory: MonthlyReportViewModelFactory

    fun createDashboardViewModelFactory(initialState: DashboardUiState): DashboardViewModelFactory
}
