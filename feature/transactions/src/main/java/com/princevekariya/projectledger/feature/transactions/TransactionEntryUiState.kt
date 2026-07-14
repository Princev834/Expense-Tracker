package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.common.UiMessage
import com.princevekariya.projectledger.core.model.FinancialAccount
import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionCategory
import com.princevekariya.projectledger.core.model.TransactionType

data class TransactionEntryUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val noteInput: String = "",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val accounts: List<FinancialAccount> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val isLoadingReferences: Boolean = true,
    val isSaving: Boolean = false,
    val userMessage: UiMessage? = null,
) {
    val parsedAmount: Money?
        get() = Money.parseMajorUnits(amountInput).getOrNull()

    val canSave: Boolean
        get() = !isLoadingReferences &&
            !isSaving &&
            selectedAccountId != null &&
            selectedCategoryId != null &&
            parsedAmount?.isPositive == true
}
