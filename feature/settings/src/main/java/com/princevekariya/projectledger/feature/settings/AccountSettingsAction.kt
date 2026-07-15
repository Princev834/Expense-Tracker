package com.princevekariya.projectledger.feature.settings

import com.princevekariya.projectledger.core.model.AccountType

sealed interface AccountSettingsAction {
    data object AddAccountRequested : AccountSettingsAction

    data object CancelAccountRequested : AccountSettingsAction

    data class AccountNameChanged(
        val value: String,
    ) : AccountSettingsAction

    data class OpeningBalanceChanged(
        val value: String,
    ) : AccountSettingsAction

    data class AccountTypeSelected(
        val value: AccountType,
    ) : AccountSettingsAction

    data object SaveAccountRequested : AccountSettingsAction

    data object RetryRequested : AccountSettingsAction

    data class MessageShown(
        val id: Long,
    ) : AccountSettingsAction
}
