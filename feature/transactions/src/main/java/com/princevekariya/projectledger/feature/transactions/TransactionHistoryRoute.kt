package com.princevekariya.projectledger.feature.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TransactionHistoryRoute(factory: TransactionHistoryViewModelFactory, modifier: Modifier = Modifier) {
    val transactionHistoryViewModel: TransactionHistoryViewModel = viewModel(
        factory = factory,
    )
    val state by transactionHistoryViewModel.uiState
        .collectAsStateWithLifecycle()

    TransactionHistoryScreen(
        state = state,
        onAction = transactionHistoryViewModel::onAction,
        modifier = modifier,
    )
}
