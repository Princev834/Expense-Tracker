package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.database.repository.LedgerRepositories

class DefaultAppContainer(
    override val appLogger: AppLogger,
    override val repositories: LedgerRepositories,
) : AppContainer
