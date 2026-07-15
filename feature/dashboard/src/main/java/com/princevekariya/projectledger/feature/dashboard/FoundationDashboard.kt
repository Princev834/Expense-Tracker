package com.princevekariya.projectledger.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.designsystem.component.LedgerEmptyState
import com.princevekariya.projectledger.core.designsystem.component.LedgerErrorState
import com.princevekariya.projectledger.core.designsystem.component.LedgerLoadingState
import com.princevekariya.projectledger.core.designsystem.component.LedgerMetricCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerMetricTone
import com.princevekariya.projectledger.core.designsystem.component.LedgerPrimaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSecondaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerTransactionDirection
import com.princevekariya.projectledger.core.designsystem.component.LedgerTransactionRow
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.TransactionType

@Composable
fun FoundationDashboard(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = spacing.screenHorizontal,
                vertical = spacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        DashboardHeader(state = state)
        DashboardBody(
            state = state,
            onAction = onAction,
            onAddExpense = onAddExpense,
            onAddIncome = onAddIncome,
        )
        Text(
            text = "Phase 22 - live Room dashboard",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun DashboardBody(
    state: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
) {
    when (val loadState = state.loadState) {
        UiLoadState.Idle,
        UiLoadState.Loading,
        -> LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            LedgerLoadingState(message = "Loading your ledger")
        }
        UiLoadState.Content -> DashboardContent(
            state = state,
            onAddExpense = onAddExpense,
            onAddIncome = onAddIncome,
        )
        is UiLoadState.Error -> LedgerSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            LedgerErrorState(
                title = "Unable to load dashboard",
                message = loadState.message,
                onRetry = {
                    onAction(DashboardAction.RetryRequested)
                },
            )
        }
    }
}

@Composable
private fun DashboardContent(state: DashboardUiState, onAddExpense: () -> Unit, onAddIncome: () -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        BalanceSection(state = state)
        MetricSection(state = state)
        ActionSection(
            onAddExpense = onAddExpense,
            onAddIncome = onAddIncome,
        )
        TransactionSection(
            transactions = state.recentTransactions,
        )
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = "Project Ledger",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Your balance and recent activity update automatically.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${state.variant.displayName} - " +
                "${state.platformDescription} - " +
                "${state.moduleCount} modules",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BalanceSection(state: DashboardUiState) {
    val supportingText = when (state.activeAccountCount) {
        1 -> "1 active account"
        else -> "${state.activeAccountCount} active accounts"
    }

    LedgerMetricCard(
        title = "Total balance",
        value = state.totalBalance.formatted(),
        supportingText = supportingText,
        tone = LedgerMetricTone.NEUTRAL,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MetricSection(state: DashboardUiState) {
    val spacing = MaterialTheme.ledgerSpacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        LedgerMetricCard(
            title = "Income",
            value = state.incomeThisMonth.formatted(),
            supportingText = "This month",
            tone = LedgerMetricTone.INCOME,
            modifier = Modifier.weight(1f),
        )
        LedgerMetricCard(
            title = "Expenses",
            value = state.expensesThisMonth.formatted(),
            supportingText = "This month",
            tone = LedgerMetricTone.EXPENSE,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionSection(onAddExpense: () -> Unit, onAddIncome: () -> Unit) {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Quick actions")
        LedgerPrimaryButton(
            label = "Add expense",
            onClick = onAddExpense,
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerSecondaryButton(
            label = "Add income",
            onClick = onAddIncome,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TransactionSection(transactions: List<DashboardTransactionItem>) {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Recent activity")
        if (transactions.isEmpty()) {
            LedgerEmptyState(
                title = "No transactions yet",
                message = "Add an expense or income to see it here.",
            )
        } else {
            transactions.forEach { transaction ->
                LedgerTransactionRow(
                    title = transaction.title,
                    subtitle = transaction.subtitle,
                    amount = transaction.signedAmount(),
                    direction = transaction.type.toDirection(),
                )
            }
        }
    }
}

private fun DashboardTransactionItem.signedAmount(): String = when (type) {
    TransactionType.EXPENSE -> "- ${amount.formatted()}"
    TransactionType.INCOME -> "+ ${amount.formatted()}"
    TransactionType.TRANSFER -> amount.formatted()
}

private fun TransactionType.toDirection(): LedgerTransactionDirection = when (this) {
    TransactionType.EXPENSE -> LedgerTransactionDirection.EXPENSE
    TransactionType.INCOME -> LedgerTransactionDirection.INCOME
    TransactionType.TRANSFER -> LedgerTransactionDirection.TRANSFER
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
