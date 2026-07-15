package com.princevekariya.projectledger.domain.transactions.account

fun interface AccountIdGenerator {
    fun generateId(): String
}
