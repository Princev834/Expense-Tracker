package com.princevekariya.projectledger.domain.transactions.command

fun interface TransactionIdGenerator {
    fun generateId(): String
}
