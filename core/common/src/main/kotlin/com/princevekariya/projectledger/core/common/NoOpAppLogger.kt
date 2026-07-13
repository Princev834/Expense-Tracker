package com.princevekariya.projectledger.core.common

object NoOpAppLogger : AppLogger {
    override fun log(level: AppLogLevel, event: String, message: String, throwable: Throwable?) = Unit
}
