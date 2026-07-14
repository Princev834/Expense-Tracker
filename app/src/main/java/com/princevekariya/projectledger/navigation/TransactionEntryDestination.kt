package com.princevekariya.projectledger.navigation

import com.princevekariya.projectledger.core.model.TransactionType
import java.util.Locale

object TransactionEntryDestination {
    const val TRANSACTION_TYPE_ARGUMENT: String = "transactionType"
    const val ROUTE_PATTERN: String = "transaction-entry/{transactionType}"
    const val DEEP_LINK_PATTERN: String = "projectledger://entry/{transactionType}"

    fun createRoute(type: TransactionType): String = "transaction-entry/${type.name.lowercase(Locale.ENGLISH)}"

    fun parseType(value: String?): TransactionType = when (
        value?.lowercase(Locale.ENGLISH)
    ) {
        "income" -> TransactionType.INCOME
        else -> TransactionType.EXPENSE
    }
}
