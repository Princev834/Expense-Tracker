package com.princevekariya.projectledger.core.model

data class LedgerTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Money,
    val accountId: String,
    val occurredAtEpochMillis: Long,
    val paymentMethod: PaymentMethod,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val merchantId: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val note: String? = null,
    val createdAtEpochMillis: Long = occurredAtEpochMillis,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
) {
    init {
        requireModelIdentifier(
            value = id,
            fieldName = "Transaction identifier",
        )
        requireModelIdentifier(
            value = accountId,
            fieldName = "Account identifier",
        )
        require(amount.isPositive) {
            "Transaction amount must be greater than zero."
        }
        require(occurredAtEpochMillis >= 0L) {
            "Transaction time cannot be negative."
        }
        require(createdAtEpochMillis >= 0L) {
            "Transaction creation time cannot be negative."
        }
        require(updatedAtEpochMillis >= createdAtEpochMillis) {
            "Transaction update time cannot precede its creation time."
        }
        requireOptionalModelIdentifier(
            value = destinationAccountId,
            fieldName = "Destination account identifier",
        )
        requireOptionalModelIdentifier(
            value = categoryId,
            fieldName = "Category identifier",
        )
        requireOptionalModelIdentifier(
            value = merchantId,
            fieldName = "Merchant identifier",
        )
        require(note == null || note.isNotBlank()) {
            "Transaction note cannot be blank when provided."
        }
        validateTransactionShape()
    }

    private fun validateTransactionShape() {
        when (type) {
            TransactionType.TRANSFER -> validateTransfer()
            TransactionType.EXPENSE,
            TransactionType.INCOME,
            -> validateCategorizedTransaction()
        }
    }

    private fun validateTransfer() {
        require(destinationAccountId != null) {
            "A transfer requires a destination account."
        }
        require(destinationAccountId != accountId) {
            "Transfer accounts must be different."
        }
        require(categoryId == null) {
            "A transfer cannot have an expense or income category."
        }
        require(merchantId == null) {
            "A transfer cannot have a merchant."
        }
    }

    private fun validateCategorizedTransaction() {
        require(destinationAccountId == null) {
            "Only transfers can have a destination account."
        }
        require(categoryId != null) {
            "Expense and income transactions require a category."
        }
    }
}
