package com.princevekariya.projectledger.feature.reports

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.Money
import java.time.YearMonth

data class MonthlyReportUiState(
    val selectedMonth: YearMonth,
    val currentMonth: YearMonth,
    val selectedMonthLabel: String,
    val income: Money = Money.zero(),
    val expenses: Money = Money.zero(),
    val netCashFlow: Money = Money.zero(),
    val transactionCount: Int = 0,
    val categoryExpenses: List<MonthlyCategoryExpenseItem> = emptyList(),
    val loadState: UiLoadState = UiLoadState.Loading,
) {
    val canMoveNext: Boolean
        get() = selectedMonth < currentMonth

    val hasActivity: Boolean
        get() = transactionCount > 0
}
