package com.princevekariya.projectledger.feature.reports

sealed interface MonthlyReportAction {
    data object PreviousMonthRequested : MonthlyReportAction

    data object NextMonthRequested : MonthlyReportAction

    data object RetryRequested : MonthlyReportAction
}
