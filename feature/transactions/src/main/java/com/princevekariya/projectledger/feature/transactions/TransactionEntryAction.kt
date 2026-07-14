package com.princevekariya.projectledger.feature.transactions

import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionType

sealed interface TransactionEntryAction {
    data class TransactionTypeChanged(
        val value: TransactionType,
    ) : TransactionEntryAction

    data class AmountChanged(
        val value: String,
    ) : TransactionEntryAction

    data class NoteChanged(
        val value: String,
    ) : TransactionEntryAction

    data class AccountSelected(
        val accountId: String,
    ) : TransactionEntryAction

    data class CategorySelected(
        val categoryId: String,
    ) : TransactionEntryAction

    data class PaymentMethodSelected(
        val value: PaymentMethod,
    ) : TransactionEntryAction

    data object SaveClicked : TransactionEntryAction

    data object RetryReferences : TransactionEntryAction

    data class MessageShown(
        val id: Long,
    ) : TransactionEntryAction
}
