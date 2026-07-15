package com.princevekariya.projectledger.feature.transactions

sealed interface TransactionHistoryAction {
    data class FilterSelected(
        val filter: TransactionHistoryFilter,
    ) : TransactionHistoryAction

    data object RetryRequested : TransactionHistoryAction
}
