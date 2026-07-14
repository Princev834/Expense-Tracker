package com.princevekariya.projectledger.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.domain.transactions.command.SaveManualTransactionUseCase
import com.princevekariya.projectledger.domain.transactions.repository.AccountRepository
import com.princevekariya.projectledger.domain.transactions.repository.CategoryRepository

class TransactionEntryViewModelFactory(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val saveManualTransaction: SaveManualTransactionUseCase,
    private val appLogger: AppLogger,
    private val initialTransactionType: TransactionType = TransactionType.EXPENSE,
) : ViewModelProvider.Factory {
    fun forTransactionType(type: TransactionType): TransactionEntryViewModelFactory = TransactionEntryViewModelFactory(
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        saveManualTransaction = saveManualTransaction,
        appLogger = appLogger,
        initialTransactionType = type,
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionEntryViewModel(
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
                saveManualTransaction = saveManualTransaction,
                appLogger = appLogger,
                initialTransactionType = initialTransactionType,
            ) as T
        }

        throw IllegalArgumentException(
            "Unsupported ViewModel class: ${modelClass.name}",
        )
    }
}
