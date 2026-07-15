package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.TransactionType

data class TransactionHistoryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Money,
    val type: TransactionType,
)
