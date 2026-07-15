package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.common.UiLoadState

data class TransactionHistoryUiState(
    val selectedFilter: TransactionHistoryFilter =
        TransactionHistoryFilter.ALL,
    val transactions: List<TransactionHistoryItem> = emptyList(),
    val totalTransactionCount: Int = 0,
    val loadState: UiLoadState = UiLoadState.Loading,
)
