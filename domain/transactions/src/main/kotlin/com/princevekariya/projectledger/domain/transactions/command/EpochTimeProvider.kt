package com.princevekariya.projectledger.domain.transactions.command

fun interface EpochTimeProvider {
    fun currentTimeMillis(): Long
}
