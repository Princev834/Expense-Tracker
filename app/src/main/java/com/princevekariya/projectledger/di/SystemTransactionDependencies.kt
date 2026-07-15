package com.princevekariya.projectledger.di

import com.princevekariya.projectledger.domain.transactions.account.AccountIdGenerator
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import com.princevekariya.projectledger.domain.transactions.command.TransactionIdGenerator
import java.util.UUID

object SystemEpochTimeProvider : EpochTimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

object UuidTransactionIdGenerator : TransactionIdGenerator {
    override fun generateId(): String = "transaction-${UUID.randomUUID()}"
}

object UuidAccountIdGenerator : AccountIdGenerator {
    override fun generateId(): String = "account-${UUID.randomUUID()}"
}
