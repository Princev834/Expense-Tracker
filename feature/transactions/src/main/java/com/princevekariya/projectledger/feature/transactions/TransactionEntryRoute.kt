package com.princevekariya.projectledger.feature.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.TransactionType

@Composable
fun TransactionEntryRoute(
    factory: TransactionEntryViewModelFactory,
    initialTransactionType: TransactionType,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typedFactory = remember(factory, initialTransactionType) {
        factory.forTransactionType(type = initialTransactionType)
    }
    val transactionEntryViewModel: TransactionEntryViewModel = viewModel(
        key = "transaction-entry-${initialTransactionType.name}",
        factory = typedFactory,
    )
    val state by transactionEntryViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = state.userMessage
    val spacing = MaterialTheme.ledgerSpacing

    LaunchedEffect(userMessage?.id) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(message = userMessage.text)
            transactionEntryViewModel.onAction(
                TransactionEntryAction.MessageShown(id = userMessage.id),
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        TransactionEntryScreen(
            state = state,
            onAction = transactionEntryViewModel::onAction,
            onNavigateBack = onNavigateBack,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(spacing.medium),
        )
    }
}
