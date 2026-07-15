package com.princevekariya.projectledger.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.designsystem.component.LedgerEmptyState
import com.princevekariya.projectledger.core.designsystem.component.LedgerErrorState
import com.princevekariya.projectledger.core.designsystem.component.LedgerLoadingState
import com.princevekariya.projectledger.core.designsystem.component.LedgerMetricCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerMetricTone
import com.princevekariya.projectledger.core.designsystem.component.LedgerSecondaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.theme.ledgerColors
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.Money

@Composable
fun MonthlyReportScreen(
    state: MonthlyReportUiState,
    onAction: (MonthlyReportAction) -> Unit,
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
            MonthlyReportHeader()
        }
        item {
            MonthSelector(
                state = state,
                onAction = onAction,
            )
        }

        when (val loadState = state.loadState) {
            UiLoadState.Idle,
            UiLoadState.Loading,
            -> item {
                LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    LedgerLoadingState(
                        message = "Preparing your monthly report",
                    )
                }
            }
            UiLoadState.Content -> {
                item {
                    MonthlySummary(state = state)
                }
                item {
                    MonthlyActivityCard(state = state)
                }
                if (!state.hasActivity) {
                    item {
                        LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            LedgerEmptyState(
                                title = "No activity this month",
                                message = "Add income or expenses to build this report.",
                            )
                        }
                    }
                } else if (state.categoryExpenses.isEmpty()) {
                    item {
                        LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            LedgerEmptyState(
                                title = "No expenses this month",
                                message = "Income exists, but there is no spending to break down.",
                            )
                        }
                    }
                } else {
                    item {
                        SectionTitle(title = "Spending by category")
                    }
                    items(
                        items = state.categoryExpenses,
                        key = { category -> category.id },
                    ) { category ->
                        CategoryExpenseCard(category = category)
                    }
                }
            }
            is UiLoadState.Error -> item {
                LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    LedgerErrorState(
                        title = "Unable to load reports",
                        message = loadState.message,
                        onRetry = {
                            onAction(MonthlyReportAction.RetryRequested)
                        },
                    )
                }
            }
        }

        item {
            Text(
                text = "Phase 24 - live monthly reports",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun MonthlyReportHeader() {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Monthly income, spending, and cash-flow overview.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthSelector(state: MonthlyReportUiState, onAction: (MonthlyReportAction) -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Selected month",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = state.selectedMonthLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            LedgerSecondaryButton(
                label = "Previous month",
                onClick = {
                    onAction(
                        MonthlyReportAction.PreviousMonthRequested,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            LedgerSecondaryButton(
                label = "Next month",
                onClick = {
                    onAction(MonthlyReportAction.NextMonthRequested)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canMoveNext,
            )
        }
    }
}

@Composable
private fun MonthlySummary(state: MonthlyReportUiState) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        SectionTitle(title = "Monthly summary")
        LedgerMetricCard(
            title = "Income",
            value = state.income.formatted(),
            supportingText = "Money received in ${state.selectedMonthLabel}",
            tone = LedgerMetricTone.INCOME,
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerMetricCard(
            title = "Expenses",
            value = state.expenses.formatted(),
            supportingText = "Money spent in ${state.selectedMonthLabel}",
            tone = LedgerMetricTone.EXPENSE,
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerMetricCard(
            title = "Net cash flow",
            value = state.netCashFlow.formatted(),
            supportingText = "Income minus expenses",
            tone = state.netCashFlow.metricTone(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MonthlyActivityCard(state: MonthlyReportUiState) {
    val transactionLabel = when (state.transactionCount) {
        0 -> "No income or expense transactions"
        1 -> "1 income or expense transaction"
        else -> "${state.transactionCount} income or expense transactions"
    }

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Activity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = transactionLabel,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CategoryExpenseCard(category: MonthlyCategoryExpenseItem) {
    val expenseColor = MaterialTheme.ledgerColors.expense
    val barFraction = category.shareFraction.coerceAtLeast(
        MINIMUM_VISIBLE_BAR_FRACTION,
    )
    val transactionLabel = when (category.transactionCount) {
        1 -> "1 transaction"
        else -> "${category.transactionCount} transactions"
    }

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = category.amount.formatted(),
            style = MaterialTheme.typography.headlineSmall,
            color = expenseColor,
        )
        Text(
            text = "${category.sharePercent}% of expenses - $transactionLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CATEGORY_BAR_HEIGHT)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = barFraction)
                    .fillMaxHeight()
                    .background(expenseColor),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

private fun Money.metricTone(): LedgerMetricTone = when {
    isNegative -> LedgerMetricTone.EXPENSE
    isPositive -> LedgerMetricTone.INCOME
    else -> LedgerMetricTone.NEUTRAL
}

private val CATEGORY_BAR_HEIGHT = 8.dp
private const val MINIMUM_VISIBLE_BAR_FRACTION: Float = 0.03f
