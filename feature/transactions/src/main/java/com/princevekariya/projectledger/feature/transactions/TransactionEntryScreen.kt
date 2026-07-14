package com.princevekariya.projectledger.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.princevekariya.projectledger.core.designsystem.component.LedgerAmountField
import com.princevekariya.projectledger.core.designsystem.component.LedgerErrorState
import com.princevekariya.projectledger.core.designsystem.component.LedgerLoadingState
import com.princevekariya.projectledger.core.designsystem.component.LedgerPrimaryButton
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionType

@Composable
fun TransactionEntryScreen(
    state: TransactionEntryUiState,
    onAction: (TransactionEntryAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                horizontal = spacing.screenHorizontal,
                vertical = spacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        EntryHeader(
            transactionType = state.transactionType,
            onNavigateBack = onNavigateBack,
        )
        TransactionTypeSelector(
            selectedType = state.transactionType,
            enabled = !state.isSaving,
            onTypeSelected = { type ->
                onAction(
                    TransactionEntryAction.TransactionTypeChanged(
                        value = type,
                    ),
                )
            },
        )
        TransactionDetailsCard(
            state = state,
            onAction = onAction,
        )
        LedgerPrimaryButton(
            label = state.saveLabel(),
            onClick = {
                onAction(TransactionEntryAction.SaveClicked)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSave,
            loading = state.isSaving,
        )
        Text(
            text = "Phase 20 - real transaction entry screen",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun TransactionDetailsCard(state: TransactionEntryUiState, onAction: (TransactionEntryAction) -> Unit) {
    LedgerSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Transaction details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        LedgerAmountField(
            value = state.amountInput,
            onValueChange = { value ->
                onAction(
                    TransactionEntryAction.AmountChanged(value = value),
                )
            },
            label = "Amount",
            errorMessage = state.amountError(),
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        ReferenceFields(
            state = state,
            onAction = onAction,
        )
        OutlinedTextField(
            value = state.noteInput,
            onValueChange = { value ->
                onAction(
                    TransactionEntryAction.NoteChanged(value = value),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            label = {
                Text(text = "Note")
            },
            placeholder = {
                Text(text = "Optional description")
            },
            minLines = 3,
            maxLines = 4,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun ReferenceFields(state: TransactionEntryUiState, onAction: (TransactionEntryAction) -> Unit) {
    val referenceProblem = state.referenceProblem()

    when {
        state.isLoadingReferences &&
            state.accounts.isEmpty() &&
            state.categories.isEmpty() -> {
            LedgerLoadingState(
                message = "Preparing accounts and categories",
            )
        }
        referenceProblem != null -> {
            LedgerErrorState(
                title = "Entry setup is incomplete",
                message = referenceProblem,
                onRetry = {
                    onAction(TransactionEntryAction.RetryReferences)
                },
            )
        }
        else -> {
            AccountDropdown(
                accounts = state.accounts,
                selectedAccountId = state.selectedAccountId,
                enabled = !state.isSaving,
                onAccountSelected = { accountId ->
                    onAction(
                        TransactionEntryAction.AccountSelected(
                            accountId = accountId,
                        ),
                    )
                },
            )
            CategoryDropdown(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                enabled = !state.isSaving,
                onCategorySelected = { categoryId ->
                    onAction(
                        TransactionEntryAction.CategorySelected(
                            categoryId = categoryId,
                        ),
                    )
                },
            )
            PaymentMethodDropdown(
                selectedMethod = state.paymentMethod,
                enabled = !state.isSaving,
                onPaymentMethodSelected = { paymentMethod ->
                    onAction(
                        TransactionEntryAction.PaymentMethodSelected(
                            value = paymentMethod,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun EntryHeader(transactionType: TransactionType, onNavigateBack: () -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        TextButton(onClick = onNavigateBack) {
            Text(text = "Back")
        }
        Text(
            text = transactionType.screenTitle(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Enter the details below. Nothing is saved until you confirm.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    enabled: Boolean,
    onTypeSelected: (TransactionType) -> Unit,
) {
    val spacing = MaterialTheme.ledgerSpacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        listOf(
            TransactionType.EXPENSE,
            TransactionType.INCOME,
        ).forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = {
                    onTypeSelected(type)
                },
                label = {
                    Text(text = type.displayLabel())
                },
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun AccountDropdown(
    accounts: List<FinancialAccount>,
    selectedAccountId: String?,
    enabled: Boolean,
    onAccountSelected: (String) -> Unit,
) {
    EntryDropdownField(
        label = "Account",
        options = accounts,
        selectedOption = accounts.firstOrNull { account ->
            account.id == selectedAccountId
        },
        optionLabel = { account ->
            account.name
        },
        enabled = enabled,
        onOptionSelected = { account ->
            onAccountSelected(account.id)
        },
    )
}

@Composable
private fun CategoryDropdown(
    categories: List<TransactionCategory>,
    selectedCategoryId: String?,
    enabled: Boolean,
    onCategorySelected: (String) -> Unit,
) {
    EntryDropdownField(
        label = "Category",
        options = categories,
        selectedOption = categories.firstOrNull { category ->
            category.id == selectedCategoryId
        },
        optionLabel = { category ->
            category.name
        },
        enabled = enabled,
        onOptionSelected = { category ->
            onCategorySelected(category.id)
        },
    )
}

@Composable
private fun PaymentMethodDropdown(
    selectedMethod: PaymentMethod,
    enabled: Boolean,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
) {
    EntryDropdownField(
        label = "Payment method",
        options = PaymentMethod.values().toList(),
        selectedOption = selectedMethod,
        optionLabel = { method ->
            method.displayLabel()
        },
        enabled = enabled,
        onOptionSelected = onPaymentMethodSelected,
    )
}

@Composable
private fun <T> EntryDropdownField(
    label: String,
    options: List<T>,
    selectedOption: T?,
    optionLabel: (T) -> String,
    enabled: Boolean,
    onOptionSelected: (T) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && options.isNotEmpty(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = selectedOption?.let(optionLabel) ?: "Select $label",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = "▼")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(text = optionLabel(option))
                        },
                        onClick = {
                            expanded = false
                            onOptionSelected(option)
                        },
                    )
                }
            }
        }
    }
}

private fun TransactionEntryUiState.referenceProblem(): String? = when {
    !isLoadingReferences && accounts.isEmpty() ->
        "No active account is available. The Cash account may still be initializing."
    !isLoadingReferences && categories.isEmpty() ->
        "No active category is available for ${transactionType.displayLabel().lowercase()}."
    else -> null
}

private fun TransactionEntryUiState.amountError(): String? = when {
    amountInput.isBlank() -> null
    parsedAmount == null -> "Enter a valid amount with at most two decimal places."
    parsedAmount?.isPositive != true -> "Amount must be greater than zero."
    else -> null
}

private fun TransactionEntryUiState.saveLabel(): String = when (transactionType) {
    TransactionType.EXPENSE -> "Save expense"
    TransactionType.INCOME -> "Save income"
    TransactionType.TRANSFER -> "Save transaction"
}

private fun TransactionType.screenTitle(): String = when (this) {
    TransactionType.EXPENSE -> "Add expense"
    TransactionType.INCOME -> "Add income"
    TransactionType.TRANSFER -> "Add transaction"
}

private fun TransactionType.displayLabel(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
}

private fun PaymentMethod.displayLabel(): String = when (this) {
    PaymentMethod.CASH -> "Cash"
    PaymentMethod.UPI -> "UPI"
    PaymentMethod.DEBIT_CARD -> "Debit card"
    PaymentMethod.CREDIT_CARD -> "Credit card"
    PaymentMethod.BANK_TRANSFER -> "Bank transfer"
    PaymentMethod.DIGITAL_WALLET -> "Digital wallet"
    PaymentMethod.OTHER -> "Other"
}
