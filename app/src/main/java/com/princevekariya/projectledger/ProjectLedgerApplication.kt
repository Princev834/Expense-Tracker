package com.princevekariya.projectledger

import android.app.Application
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.platform.device.AndroidAppLogger
import com.princevekariya.projectledger.platform.device.AndroidProcessErrorReporter

class ProjectLedgerApplication : Application() {
    lateinit var appLogger: AppLogger
        private set

    override fun onCreate() {
        super.onCreate()

        appLogger = AndroidAppLogger(
            verboseLoggingEnabled = BuildConfig.ENABLE_VERBOSE_LOGGING,
            includeThrowableDetails = BuildConfig.DEBUG,
        )
        AndroidProcessErrorReporter(logger = appLogger).install()
        appLogger.info(
            event = "application_started",
            message = "Application logging and process error reporting are ready.",
        )
    }
}
