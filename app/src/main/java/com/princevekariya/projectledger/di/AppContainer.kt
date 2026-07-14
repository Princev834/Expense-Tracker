package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase

interface AppContainer {
    val appLogger: AppLogger

    val repositories: LedgerRepositories

    val ensureDefaultLedgerData: EnsureDefaultLedgerDataUseCase

    val saveManualTransaction: SaveManualTransactionUseCase
}
