package com.princevekariya.projectledger.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.domain.transactions.account.CreateFinancialAccountUseCase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository

class AccountSettingsViewModelFactory(
    private val accountRepository: AccountRepository,
    private val createFinancialAccount: CreateFinancialAccountUseCase,
    private val appLogger: AppLogger,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(
                AccountSettingsViewModel::class.java,
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return AccountSettingsViewModel(
                accountRepository = accountRepository,
                createFinancialAccount = createFinancialAccount,
                appLogger = appLogger,
            ) as T
        }

        throw IllegalArgumentException(
            "Unsupported ViewModel class: ${modelClass.name}",
        )
    }
}
