package com.princevekariya.projectledger.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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
import com.princevekariya.projectledger.core.designsystem.component.LedgerPrimaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSecondaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.component.LedgerTextField
import com.princevekariya.projectledger.core.designsystem.theme.ledgerColors
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.AccountType

@Composable
fun AccountSettingsScreen(
    state: AccountSettingsUiState,
    onAction: (AccountSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.ledgerSpacing

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = spacing.screenHorizontal,
            vertical = spacing.screenVertical,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        item {
            SettingsHeader()
        }
        item {
            AccountsSectionHeader(
                isFormVisible = state.isFormVisible,
                onAddAccount = {
                    onAction(AccountSettingsAction.AddAccountRequested)
                },
            )
        }
        if (state.isFormVisible) {
            item {
                AddAccountForm(
                    state = state,
                    onAction = onAction,
                )
            }
        }

        when (val loadState = state.loadState) {
            UiLoadState.Idle,
            UiLoadState.Loading,
            -> item {
                LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    LedgerLoadingState(message = "Loading accounts")
                }
            }
            UiLoadState.Content -> {
                if (state.accounts.isEmpty()) {
                    item {
                        LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            LedgerEmptyState(
                                title = "No accounts",
                                message = "Create an account to start tracking money.",
                            )
                        }
                    }
                } else {
                    items(
                        items = state.accounts,
                        key = { account -> account.id },
                    ) { account ->
                        AccountCard(account = account)
                    }
                }
            }
            is UiLoadState.Error -> item {
                LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    LedgerErrorState(
                        title = "Unable to load accounts",
                        message = loadState.message,
                        onRetry = {
                            onAction(AccountSettingsAction.RetryRequested)
                        },
                    )
                }
            }
        }

        item {
            Text(
                text = "Phase 25 - account management",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun SettingsHeader() {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Manage the financial accounts used across Project Ledger.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountsSectionHeader(isFormVisible: Boolean, onAddAccount: () -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (!isFormVisible) {
            LedgerPrimaryButton(
                label = "Add account",
                onClick = onAddAccount,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AddAccountForm(state: AccountSettingsUiState, onAction: (AccountSettingsAction) -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "New account",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        LedgerTextField(
            value = state.accountNameInput,
            onValueChange = { value ->
                onAction(
                    AccountSettingsAction.AccountNameChanged(
                        value = value,
                    ),
                )
            },
            label = "Account name",
            placeholder = "Example: HDFC Bank",
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerAmountField(
            value = state.openingBalanceInput,
            onValueChange = { value ->
                onAction(
                    AccountSettingsAction.OpeningBalanceChanged(
                        value = value,
                    ),
                )
            },
            label = "Opening balance",
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Account type",
            style = MaterialTheme.typography.titleMedium,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            AccountType.values().forEach { type ->
                FilterChip(
                    selected = state.selectedAccountType == type,
                    onClick = {
                        onAction(
                            AccountSettingsAction.AccountTypeSelected(
                                value = type,
                            ),
                        )
                    },
                    enabled = !state.isSaving,
                    label = {
                        Text(text = type.displayLabel())
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        LedgerPrimaryButton(
            label = "Save account",
            onClick = {
                onAction(AccountSettingsAction.SaveAccountRequested)
            },
            enabled = state.canSave,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        LedgerSecondaryButton(
            label = "Cancel",
            onClick = {
                onAction(AccountSettingsAction.CancelAccountRequested)
            },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AccountCard(account: AccountSettingsItem) {
    val balanceColor = when {
        account.currentBalance.isNegative ->
            MaterialTheme.ledgerColors.expense
        else -> MaterialTheme.colorScheme.primary
    }
    val status = if (account.isArchived) {
        "Archived"
    } else {
        "Active"
    }

    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = account.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = account.type.displayLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = account.currentBalance.formatted(),
            style = MaterialTheme.typography.headlineSmall,
            color = balanceColor,
        )
        Text(
            text = "Opening balance: ${account.openingBalance.formatted()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

internal fun AccountType.displayLabel(): String = when (this) {
    AccountType.CASH -> "Cash"
    AccountType.BANK_ACCOUNT -> "Bank account"
    AccountType.CREDIT_CARD -> "Credit card"
    AccountType.DIGITAL_WALLET -> "Digital wallet"
    AccountType.OTHER -> "Other"
}
