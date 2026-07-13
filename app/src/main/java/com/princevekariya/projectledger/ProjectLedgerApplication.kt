package com.princevekariya.projectledger

import android.app.Application
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.database.ProjectLedgerDatabase
import com.princevekariya.projectledger.platform.device.AndroidAppLogger
import com.princevekariya.projectledger.platform.device.AndroidProcessErrorReporter

class ProjectLedgerApplication : Application() {
    lateinit var appLogger: AppLogger
        private set

    internal lateinit var database: ProjectLedgerDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        appLogger = AndroidAppLogger(
            verboseLoggingEnabled = BuildConfig.ENABLE_VERBOSE_LOGGING,
            includeThrowableDetails = BuildConfig.DEBUG,
        )
        AndroidProcessErrorReporter(logger = appLogger).install()
        database = ProjectLedgerDatabase.create(context = this)
        appLogger.info(
            event = "application_started",
            message = "Logging, process error reporting, and the local database are ready.",
        )
    }
}
