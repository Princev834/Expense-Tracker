package com.princevekariya.projectledger.platform.device

import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.error

class AndroidProcessErrorReporter(
    private val logger: AppLogger,
) {
    fun install() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (previousHandler is LedgerUncaughtExceptionHandler) {
            return
        }

        Thread.setDefaultUncaughtExceptionHandler(
            LedgerUncaughtExceptionHandler(
                logger = logger,
                delegate = previousHandler,
            ),
        )
    }

    private class LedgerUncaughtExceptionHandler(
        private val logger: AppLogger,
        private val delegate: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching {
                logger.error(
                    event = "uncaught_exception",
                    message = "The application process terminated unexpectedly.",
                    throwable = throwable,
                )
            }

            delegate?.uncaughtException(thread, throwable)
        }
    }
}
