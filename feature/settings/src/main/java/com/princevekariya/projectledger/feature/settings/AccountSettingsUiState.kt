package com.princevekariya.projectledger.feature.settings

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.UiMessage
import com.princevekariya.projectledger.core.model.AccountType
import com.princevekariya.projectledger.core.model.Money

data class AccountSettingsUiState(
    val accounts: List<AccountSettingsItem> = emptyList(),
    val loadState: UiLoadState = UiLoadState.Loading,
    val isFormVisible: Boolean = false,
    val accountNameInput: String = "",
    val openingBalanceInput: String = "0",
    val selectedAccountType: AccountType = AccountType.CASH,
    val isSaving: Boolean = false,
    val userMessage: UiMessage? = null,
) {
    val parsedOpeningBalance: Money?
        get() = Money.parseMajorUnits(
            rawValue = openingBalanceInput,
        ).getOrNull()

    val canSave: Boolean
        get() = isFormVisible &&
            !isSaving &&
            accountNameInput.trim().isNotBlank() &&
            parsedOpeningBalance != null
}
