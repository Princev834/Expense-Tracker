package com.princevekariya.projectledger.feature.reports

import com.princevekariya.projectledger.core.model.Money

data class MonthlyCategoryExpenseItem(
    val id: String,
    val name: String,
    val amount: Money,
    val shareFraction: Float,
    val sharePercent: Int,
    val transactionCount: Int,
) {
    init {
        require(name.isNotBlank()) {
            "A report category name cannot be blank."
        }
        require(amount.isPositive) {
            "A report category amount must be positive."
        }
        require(shareFraction in 0f..1f) {
            "A report category share must be between zero and one."
        }
        require(sharePercent in 0..100) {
            "A report category percentage must be between zero and one hundred."
        }
        require(transactionCount > 0) {
            "A report category must contain at least one transaction."
        }
    }
}
