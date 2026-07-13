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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.princevekariya.projectledger.core.designsystem.component.LedgerAmountField
import com.princevekariya.projectledger.core.designsystem.component.LedgerEmptyState
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
import com.princevekariya.projectledger.core.model.AppVariantConfiguration

data class FoundationDashboardUiState(
    val variant: AppVariantConfiguration,
    val platformDescription: String,
    val moduleCount: Int,
)

@Composable
fun FoundationDashboard(state: FoundationDashboardUiState, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.ledgerSpacing
    var description by remember { mutableStateOf("Lunch at college") }
    var amount by remember { mutableStateOf("120") }

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
        MetricSection()
        ActionSection()
        InputSection(
            description = description,
            onDescriptionChange = { description = it },
            amount = amount,
            onAmountChange = { amount = it },
        )
        TransactionSection()
        StateSection()
        Text(
            text = "Phase 10 - navigation shell foundation",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun DashboardHeader(state: FoundationDashboardUiState) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Text(
            text = "Project Ledger",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "The reusable component gallery is now hosted inside the app navigation shell.",
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
private fun MetricSection() {
    val spacing = MaterialTheme.ledgerSpacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        LedgerMetricCard(
            title = "Income",
            value = "INR 12,500",
            supportingText = "This month",
            tone = LedgerMetricTone.INCOME,
            modifier = Modifier.weight(1f),
        )
        LedgerMetricCard(
            title = "Expenses",
            value = "INR 7,240",
            supportingText = "This month",
            tone = LedgerMetricTone.EXPENSE,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ActionSection() {
    val spacing = MaterialTheme.ledgerSpacing

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Actions")
        LedgerPrimaryButton(
            label = "Add expense",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerSecondaryButton(
            label = "Add income",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Button spacing: ${spacing.medium}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun StateSection() {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title = "Reusable states")
        LedgerEmptyState(
            title = "No pending reviews",
            message = "Detected transactions that need confirmation will appear here.",
        )
        LedgerLoadingState(message = "Preparing monthly summary")
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
