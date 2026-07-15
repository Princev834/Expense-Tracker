package com.princevekariya.projectledger.feature.settings

import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.Money

data class AccountSettingsItem(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalance: Money,
    val currentBalance: Money,
    val isArchived: Boolean,
)
