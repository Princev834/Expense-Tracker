package com.princevekariya.projectledger.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.designsystem.component.LedgerEmptyState
import com.princevekariya.projectledger.core.designsystem.component.LedgerErrorState
import com.princevekariya.projectledger.core.designsystem.component.LedgerLoadingState
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerTransactionDirection
import com.princevekariya.projectledger.core.designsystem.component.LedgerTransactionRow
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.TransactionType

@Composable
fun TransactionHistoryScreen(
    state: TransactionHistoryUiState,
    onAction: (TransactionHistoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.ledgerSpacing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = spacing.screenHorizontal,
                vertical = spacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        item {
            TransactionHistoryHeader(
                totalTransactionCount = state.totalTransactionCount,
            )
        }
        item {
            TransactionHistoryFilters(
                selectedFilter = state.selectedFilter,
                onFilterSelected = { filter ->
                    onAction(
                        TransactionHistoryAction.FilterSelected(
                            filter = filter,
                        ),
                    )
                },
            )
        }

        when (val loadState = state.loadState) {
            UiLoadState.Idle,
            UiLoadState.Loading,
            -> item {
                LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    LedgerLoadingState(
                        message = "Loading transaction history",
                    )
                }
            }
            UiLoadState.Content -> {
                if (state.transactions.isEmpty()) {
                    item {
                        EmptyTransactionHistory(
                            selectedFilter = state.selectedFilter,
                        )
                    }
                } else {
                    items(
                        items = state.transactions,
                        key = { transaction -> transaction.id },
                    ) { transaction ->
                        TransactionHistoryRow(
                            transaction = transaction,
                        )
                    }
                }
            }
            is UiLoadState.Error -> item {
                LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    LedgerErrorState(
                        title = "Unable to load transactions",
                        message = loadState.message,
                        onRetry = {
                            onAction(
                                TransactionHistoryAction.RetryRequested,
                            )
                        },
                    )
                }
            }
        }

        item {
            Text(
                text = "Phase 23 - live transaction history",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun TransactionHistoryHeader(totalTransactionCount: Int) {
    val spacing = MaterialTheme.ledgerSpacing
    val supportingText = when (totalTransactionCount) {
        0 -> "Your saved expenses and income will appear here."
        1 -> "1 saved transaction"
        else -> "$totalTransactionCount saved transactions"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransactionHistoryFilters(
    selectedFilter: TransactionHistoryFilter,
    onFilterSelected: (TransactionHistoryFilter) -> Unit,
) {
    val spacing = MaterialTheme.ledgerSpacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        TransactionHistoryFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = {
                    onFilterSelected(filter)
                },
                label = {
                    Text(text = filter.displayLabel())
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyTransactionHistory(selectedFilter: TransactionHistoryFilter) {
    val message = when (selectedFilter) {
        TransactionHistoryFilter.ALL ->
            "Add an expense or income to start your history."
        TransactionHistoryFilter.EXPENSE ->
            "No expenses match this filter."
        TransactionHistoryFilter.INCOME ->
            "No income transactions match this filter."
    }

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        LedgerEmptyState(
            title = "No transactions found",
            message = message,
        )
    }
}

@Composable
private fun TransactionHistoryRow(transaction: TransactionHistoryItem) {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        LedgerTransactionRow(
            title = transaction.title,
            subtitle = transaction.subtitle,
            amount = transaction.signedAmount(),
            direction = transaction.type.toDirection(),
        )
    }
}

private fun TransactionHistoryItem.signedAmount(): String = when (type) {
    TransactionType.EXPENSE -> "- ${amount.formatted()}"
    TransactionType.INCOME -> "+ ${amount.formatted()}"
    TransactionType.TRANSFER -> amount.formatted()
}

private fun TransactionType.toDirection(): LedgerTransactionDirection = when (this) {
    TransactionType.EXPENSE ->
        LedgerTransactionDirection.EXPENSE
    TransactionType.INCOME ->
        LedgerTransactionDirection.INCOME
    TransactionType.TRANSFER ->
        LedgerTransactionDirection.TRANSFER
}
