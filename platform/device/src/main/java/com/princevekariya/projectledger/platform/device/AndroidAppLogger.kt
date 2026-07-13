package com.princevekariya.projectledger.platform.device

import android.util.Log
import com.princevekariya.projectledger.core.common.AppLogLevel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.SensitiveDataRedactor

class AndroidAppLogger(
    private val verboseLoggingEnabled: Boolean,
    private val includeThrowableDetails: Boolean,
) : AppLogger {
    override fun log(level: AppLogLevel, event: String, message: String, throwable: Throwable?) {
        if (!shouldWrite(level)) {
            return
        }

        val safeEvent = SensitiveDataRedactor.redact(event).trim().ifEmpty {
            UNKNOWN_EVENT
        }
        val safeMessage = SensitiveDataRedactor.redact(message)
        val formattedMessage = "$safeEvent | $safeMessage"
        val visibleThrowable = throwable.takeIf {
            includeThrowableDetails
        }

        when (level) {
            AppLogLevel.DEBUG -> Log.d(LOG_TAG, formattedMessage, visibleThrowable)
            AppLogLevel.INFO -> Log.i(LOG_TAG, formattedMessage, visibleThrowable)
            AppLogLevel.WARNING -> Log.w(LOG_TAG, formattedMessage, visibleThrowable)
            AppLogLevel.ERROR -> Log.e(LOG_TAG, formattedMessage, visibleThrowable)
        }
    }

    private fun shouldWrite(level: AppLogLevel): Boolean = when (level) {
        AppLogLevel.DEBUG,
        AppLogLevel.INFO,
        -> verboseLoggingEnabled

        AppLogLevel.WARNING,
        AppLogLevel.ERROR,
        -> true
    }

    private companion object {
        const val LOG_TAG = "ProjectLedger"
        const val UNKNOWN_EVENT = "unknown_event"
    }
}
