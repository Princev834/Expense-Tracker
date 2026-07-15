package com.princevekariya.projectledger.domain.transactions.account

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.Money

data class CreateFinancialAccountDraft(
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
) {
    val normalizedName: String
        get() = name.trim()
}
