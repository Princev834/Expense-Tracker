package com.princevekariya.projectledger.core.model

data class FinancialAccount(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalance: Money = Money.zero(),
    val currentBalance: Money = openingBalance,
    val isArchived: Boolean = false,
) {
    init {
        requireModelIdentifier(
            value = id,
            fieldName = "Account identifier",
        )
        require(name.isNotBlank()) {
            "Account name cannot be blank."
        }
        require(openingBalance.currency == currentBalance.currency) {
            "Account balances must use the same currency."
        }
    }
}
