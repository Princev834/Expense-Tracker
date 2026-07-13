package com.princevekariya.projectledger.core.model

data class Budget(
    val id: String,
    val name: String,
    val limit: Money,
    val period: BudgetPeriod,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val categoryId: String? = null,
    val accountId: String? = null,
    val isEnabled: Boolean = true,
) {
    init {
        requireModelIdentifier(
            value = id,
            fieldName = "Budget identifier",
        )
        require(name.isNotBlank()) {
            "Budget name cannot be blank."
        }
        require(limit.isPositive) {
            "Budget limit must be greater than zero."
        }
        require(startEpochMillis >= 0L) {
            "Budget start time cannot be negative."
        }
        require(endEpochMillis > startEpochMillis) {
            "Budget end time must be after its start time."
        }
        requireOptionalModelIdentifier(
            value = categoryId,
            fieldName = "Category identifier",
        )
        requireOptionalModelIdentifier(
            value = accountId,
            fieldName = "Account identifier",
        )
    }
}
