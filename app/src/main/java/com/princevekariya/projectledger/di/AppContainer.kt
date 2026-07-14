package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories
import com.princevekariya.projectledger.domain.transactions.bootstrap.EnsureDefaultLedgerDataUseCase

interface AppContainer {
    val appLogger: AppLogger

    val repositories: LedgerRepositories

    val ensureDefaultLedgerData: EnsureDefaultLedgerDataUseCase
}
