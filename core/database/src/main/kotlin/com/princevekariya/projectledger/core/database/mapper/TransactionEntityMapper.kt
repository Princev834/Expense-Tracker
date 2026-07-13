package com.princevekariya.projectledger.core.database.mapper

import com.princevekariya.projectledger.core.database.entity.TransactionEntity
import com.princevekariya.projectledger.core.model.CurrencyCode
import com.princevekariya.projectledger.core.model.LedgerTransaction
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionSource
import com.princevekariya.projectledger.core.model.TransactionType

fun LedgerTransaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    transactionType = type.name,
    amountMinorUnits = amount.minorUnits,
    currencyCode = amount.currency.name,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    merchantId = merchantId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    paymentMethod = paymentMethod.name,
    source = source.name,
    note = note,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun TransactionEntity.toModel(): LedgerTransaction = LedgerTransaction(
    id = id,
    type = TransactionType.valueOf(transactionType),
    amount = Money(
        minorUnits = amountMinorUnits,
        currency = CurrencyCode.valueOf(currencyCode),
    ),
    accountId = accountId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    paymentMethod = PaymentMethod.valueOf(paymentMethod),
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    merchantId = merchantId,
    source = TransactionSource.valueOf(source),
    note = note,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)
