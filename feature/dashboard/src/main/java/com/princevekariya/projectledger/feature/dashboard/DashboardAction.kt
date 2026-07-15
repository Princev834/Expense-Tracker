package com.princevekariya.projectledger.feature.dashboard

sealed interface DashboardAction {
    data object RetryRequested : DashboardAction
}
