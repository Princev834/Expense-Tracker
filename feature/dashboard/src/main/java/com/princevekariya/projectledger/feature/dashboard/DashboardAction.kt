package com.princevekariya.projectledger.feature.dashboard

sealed interface DashboardAction {
    data class DescriptionChanged(
        val value: String,
    ) : DashboardAction

    data class AmountChanged(
        val value: String,
    ) : DashboardAction

    data object AddExpenseClicked : DashboardAction

    data object AddIncomeClicked : DashboardAction

    data object RetryRequested : DashboardAction

    data class MessageShown(
        val id: Long,
    ) : DashboardAction
}
