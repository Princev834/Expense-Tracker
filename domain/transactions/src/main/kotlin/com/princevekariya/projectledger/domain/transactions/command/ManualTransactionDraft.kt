package com.princevekariya.projectledger.domain.transactions.command

import com.princevekariya.projectledger.core.model.Money
import com.princevekariya.projectledger.core.model.PaymentMethod
import com.princevekariya.projectledger.core.model.TransactionType

data class ManualTransactionDraft(
    val type: TransactionType,
    val amount: Money,
    val accountId: String,
    val categoryId: String,
    val paymentMethod: PaymentMethod,
    val occurredAtEpochMillis: Long? = null,
    val merchantSearchKey: String? = null,
    val note: String? = null,
) {
    init {
        require(type != TransactionType.TRANSFER) {
            "Manual expense and income entry cannot create a transfer."
        }
        require(amount.isPositive) {
            "Transaction amount must be greater than zero."
        }
        require(accountId.isNotBlank()) {
            "Account identifier cannot be blank."
        }
        require(categoryId.isNotBlank()) {
            "Category identifier cannot be blank."
        }
        require(merchantSearchKey == null || merchantSearchKey.isNotBlank()) {
            "Merchant identifier cannot be blank when provided."
        }
        require(occurredAtEpochMillis == null || occurredAtEpochMillis >= 0L) {
            "Transaction time cannot be negative."
        }
    }

    val normalizedAccountId: String
        get() = accountId.trim()

    val normalizedCategoryId: String
        get() = categoryId.trim()

    val normalizedMerchantSearchKey: String?
        get() = merchantSearchKey
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }

    val normalizedNote: String?
        get() = note
            ?.trim()
            ?.takeIf { value -> value.isNotEmpty() }
}
