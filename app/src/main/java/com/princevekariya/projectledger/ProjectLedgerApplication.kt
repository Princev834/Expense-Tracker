package com.princevekariya.projectledger

import android.app.Application
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.database.ProjectLedgerDatabase
import com.princevekariya.projectledger.core.database.repository.createRepositories
import com.princevekariya.projectledger.di.AppContainer
import com.princevekariya.projectledger.di.DefaultAppContainer
import com.princevekariya.projectledger.platform.device.AndroidAppLogger
import com.princevekariya.projectledger.platform.device.AndroidProcessErrorReporter

class ProjectLedgerApplication : Application() {
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
        appContainer = DefaultAppContainer(
            appLogger = appLogger,
            repositories = database.createRepositories(),
        )

        appLogger.info(
            event = "application_container_ready",
            message = "Logging and five local repositories are ready.",
        )
    }
}
