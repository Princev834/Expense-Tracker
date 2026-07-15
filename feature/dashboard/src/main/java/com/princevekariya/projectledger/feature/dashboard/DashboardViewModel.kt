package com.princevekariya.projectledger.feature.dashboard

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

internal class DashboardViewModel(
    initialState: DashboardUiState,
    private val repositories: DashboardRepositories,
    private val dataMapper: DashboardDataMapper,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState)
    private var observationJob: Job? = null

    val uiState: StateFlow<DashboardUiState> =
        mutableUiState.asStateFlow()

    init {
        observeDashboard()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.RetryRequested -> observeDashboard()
        }
    }

    private fun observeDashboard() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            loadState = UiLoadState.Loading,
        )
        observationJob = viewModelScope.launch {
            dashboardSourceFlow()
                .catch { throwable ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loadState = UiLoadState.Error(
                            message = "Unable to load your dashboard data.",
                        ),
                    )
                    appLogger.error(
                        event = "live_dashboard_load_failed",
                        message = "Live dashboard data could not be loaded.",
                        throwable = throwable,
                    )
                }
                .collect { sourceData ->
                    mutableUiState.value = dataMapper.map(
                        baseState = mutableUiState.value,
                        sourceData = sourceData,
                    )
                }
        }
    }

    private fun dashboardSourceFlow(): Flow<DashboardSourceData> = combine(
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
        DashboardSourceData(
            accounts = accounts,
            transactions = transactions,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            merchants = merchants,
        )
    }
}
