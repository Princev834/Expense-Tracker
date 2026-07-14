package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories

interface AppContainer {
    val appLogger: AppLogger

    val repositories: LedgerRepositories
}
