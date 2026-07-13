package com.princevekariya.projectledger.core.common

interface AppLogger {
    fun log(level: AppLogLevel, event: String, message: String, throwable: Throwable? = null)
}

fun AppLogger.debug(event: String, message: String) {
    log(
        level = AppLogLevel.DEBUG,
        event = event,
        message = message,
    )
}

fun AppLogger.info(event: String, message: String) {
    log(
        level = AppLogLevel.INFO,
        event = event,
        message = message,
    )
}

fun AppLogger.warning(event: String, message: String, throwable: Throwable? = null) {
    log(
        level = AppLogLevel.WARNING,
        event = event,
        message = message,
        throwable = throwable,
    )
}

fun AppLogger.error(event: String, message: String, throwable: Throwable? = null) {
    log(
        level = AppLogLevel.ERROR,
        event = event,
        message = message,
        throwable = throwable,
    )
}
