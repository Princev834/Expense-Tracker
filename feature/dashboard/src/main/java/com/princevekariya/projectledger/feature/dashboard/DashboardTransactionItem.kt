package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.TransactionType

data class DashboardTransactionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Money,
    val type: TransactionType,
)
