package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase

class DefaultAppContainer(
    override val appLogger: AppLogger,
    override val repositories: LedgerRepositories,
    override val ensureDefaultLedgerData: EnsureDefaultLedgerDataUseCase,
    override val saveManualTransaction: SaveManualTransactionUseCase,
) : AppContainer
