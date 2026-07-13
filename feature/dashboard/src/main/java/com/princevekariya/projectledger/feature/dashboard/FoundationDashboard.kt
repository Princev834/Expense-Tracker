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
import com.princevekariya.projectledger.core.designsystem.component.LedgerAmountField
import com.princevekariya.projectledger.core.designsystem.component.LedgerEmptyState
import com.princevekariya.projectledger.core.designsystem.component.LedgerErrorState
import com.princevekariya.projectledger.core.designsystem.component.LedgerLoadingState
import com.princevekariya.projectledger.core.designsystem.component.LedgerMetricCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerMetricTone
import com.princevekariya.projectledger.core.designsystem.component.LedgerPrimaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSecondaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerTextField
import com.princevekariya.projectledger.core.designsystem.component.LedgerTransactionDirection
import com.princevekariya.projectledger.core.designsystem.component.LedgerTransactionRow
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
fun FoundationDashboard(state: DashboardUiState, onAction: (DashboardAction) -> Unit, modifier: Modifier = Modifier) {
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
        MetricSection(state = state)
        ActionSection(
            onAddExpense = {
                onAction(DashboardAction.AddExpenseClicked)
            },
            onAddIncome = {
                onAction(DashboardAction.AddIncomeClicked)
            },
        )
        InputSection(
            description = state.description,
            onDescriptionChange = {
                onAction(DashboardAction.DescriptionChanged(value = it))
            },
            amount = state.amount,
            onAmountChange = {
                onAction(DashboardAction.AmountChanged(value = it))
            },
        )
        TransactionSection()
        StateSection(
            loadState = state.loadState,
            onRetry = {
                onAction(DashboardAction.RetryRequested)
            },
        )
        Text(
            text = "Phase 14 - Room database foundation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun DashboardHeader(state: DashboardUiState) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Text(
            text = "Project Ledger",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "The dashboard now follows one-way state flow through a lifecycle-aware ViewModel.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${state.variant.displayName} - ${state.platformDescription} - ${state.moduleCount} modules",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
        SectionTitle(title = "Actions")
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
private fun InputSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
) {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Entry fields")
        LedgerTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = "Description",
            placeholder = "What did you spend on?",
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerAmountField(
            value = amount,
            onValueChange = onAmountChange,
            label = "Amount",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TransactionSection() {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Recent activity")
        LedgerTransactionRow(
            title = "College canteen",
            subtitle = "Food and Dining - Today",
            amount = "- INR 120",
            direction = LedgerTransactionDirection.EXPENSE,
        )
        LedgerTransactionRow(
            title = "Pocket money",
            subtitle = "Income - Yesterday",
            amount = "+ INR 5,000",
            direction = LedgerTransactionDirection.INCOME,
        )
        LedgerTransactionRow(
            title = "Cash withdrawal",
            subtitle = "Bank to Cash - 10 Jul",
            amount = "INR 1,000",
            direction = LedgerTransactionDirection.TRANSFER,
        )
    }
}

@Composable
private fun StateSection(loadState: UiLoadState, onRetry: () -> Unit) {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Screen state")
        when (loadState) {
            UiLoadState.Idle -> LedgerEmptyState(
                title = "Waiting to start",
                message = "This screen has not requested its data yet.",
            )
            UiLoadState.Loading -> LedgerLoadingState(
                message = "Preparing dashboard data",
            )
            UiLoadState.Content -> LedgerEmptyState(
                title = "State is ready",
                message = "Inputs, actions, and one-off messages now come from the ViewModel.",
            )
            is UiLoadState.Error -> LedgerErrorState(
                title = "Unable to load dashboard",
                message = loadState.message,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
