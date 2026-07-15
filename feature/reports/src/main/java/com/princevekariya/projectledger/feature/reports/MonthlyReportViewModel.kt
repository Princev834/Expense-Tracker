package com.princevekariya.projectledger.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.error
import com.princevekariya.projectledger.core.model.CategoryType
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class MonthlyReportViewModel(
    private val repositories: MonthlyReportRepositories,
    private val dataMapper: MonthlyReportDataMapper,
    private val appLogger: AppLogger,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        dataMapper.initialState(),
    )
    private var observationJob: Job? = null
    private var latestSourceData: MonthlyReportSourceData? = null

    val uiState: StateFlow<MonthlyReportUiState> =
        mutableUiState.asStateFlow()

    init {
        observeReportData()
    }

    fun onAction(action: MonthlyReportAction) {
        when (action) {
            MonthlyReportAction.PreviousMonthRequested -> {
                moveToMonth(
                    month = mutableUiState.value.selectedMonth.minusMonths(1),
                )
            }
            MonthlyReportAction.NextMonthRequested -> {
                val state = mutableUiState.value
                if (state.canMoveNext) {
                    moveToMonth(
                        month = state.selectedMonth.plusMonths(1),
                    )
                }
            }
            MonthlyReportAction.RetryRequested -> {
                observeReportData()
            }
        }
    }

    private fun observeReportData() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            loadState = UiLoadState.Loading,
        )
        observationJob = viewModelScope.launch {
            reportSourceFlow()
                .catch { throwable ->
                    mutableUiState.value = mutableUiState.value.copy(
                        loadState = UiLoadState.Error(
                            message = "Unable to load your monthly report.",
                        ),
                    )
                    appLogger.error(
                        event = "monthly_report_load_failed",
                        message = "Monthly report data could not be loaded.",
                        throwable = throwable,
                    )
                }
                .collect { sourceData ->
                    latestSourceData = sourceData
                    mutableUiState.value = dataMapper.map(
                        sourceData = sourceData,
                        selectedMonth =
                        mutableUiState.value.selectedMonth,
                    )
                }
        }
    }

    private fun moveToMonth(month: YearMonth) {
        val state = mutableUiState.value
        if (month > state.currentMonth) {
            return
        }

        val sourceData = latestSourceData
        mutableUiState.value = if (sourceData == null) {
            dataMapper.loadingStateFor(selectedMonth = month)
        } else {
            dataMapper.map(
                sourceData = sourceData,
                selectedMonth = month,
            )
        }
    }

    private fun reportSourceFlow(): Flow<MonthlyReportSourceData> = combine(
        repositories.transactions.observeAll(),
        repositories.categories.observeActive(
            type = CategoryType.EXPENSE,
        ),
    ) { transactions, expenseCategories ->
        MonthlyReportSourceData(
            transactions = transactions,
            expenseCategories = expenseCategories,
        )
    }
}
