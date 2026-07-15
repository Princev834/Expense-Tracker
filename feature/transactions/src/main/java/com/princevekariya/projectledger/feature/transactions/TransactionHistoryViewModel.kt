package com.princevekariya.projectledger.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.error
import com.princevekariya.projectledger.core.model.CategoryType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class TransactionHistoryViewModel(
    private val repositories: TransactionHistoryRepositories,
    private val dataMapper: TransactionHistoryDataMapper,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        TransactionHistoryUiState(),
    )
    private var observationJob: Job? = null
    private var latestSourceData: TransactionHistorySourceData? = null

    val uiState: StateFlow<TransactionHistoryUiState> =
        mutableUiState.asStateFlow()

    init {
        observeTransactions()
    }

    fun onAction(action: TransactionHistoryAction) {
        when (action) {
            is TransactionHistoryAction.FilterSelected -> {
                applyFilter(filter = action.filter)
            }
            TransactionHistoryAction.RetryRequested -> {
                observeTransactions()
            }
        }
    }

    private fun observeTransactions() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            loadState = UiLoadState.Loading,
        )
        observationJob = viewModelScope.launch {
            transactionSourceFlow()
                .catch { throwable ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loadState = UiLoadState.Error(
                            message = "Unable to load your transaction history.",
                        ),
                    )
                    appLogger.error(
                        event = "transaction_history_load_failed",
                        message = "Transaction history data could not be loaded.",
                        throwable = throwable,
                    )
                }
                .collect { sourceData ->
                    latestSourceData = sourceData
                    mutableUiState.value = dataMapper.map(
                        sourceData = sourceData,
                        selectedFilter =
                        mutableUiState.value.selectedFilter,
                    )
                }
        }
    }

    private fun applyFilter(filter: TransactionHistoryFilter) {
        if (filter == mutableUiState.value.selectedFilter) {
            return
        }

        val sourceData = latestSourceData
        mutableUiState.value = if (sourceData == null) {
            mutableUiState.value.copy(selectedFilter = filter)
        } else {
            dataMapper.map(
                sourceData = sourceData,
                selectedFilter = filter,
            )
        }
    }

    private fun transactionSourceFlow(): Flow<TransactionHistorySourceData> = combine(
        repositories.accounts.observeAll(),
        repositories.transactions.observeAll(),
        repositories.categories.observeActive(
            type = CategoryType.EXPENSE,
        ),
        repositories.categories.observeActive(
            type = CategoryType.INCOME,
        ),
        repositories.merchants.observeActive(),
    ) {
            accounts,
            transactions,
            expenseCategories,
            incomeCategories,
            merchants,
        ->
        TransactionHistorySourceData(
            accounts = accounts,
            transactions = transactions,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            merchants = merchants,
        )
    }
}
