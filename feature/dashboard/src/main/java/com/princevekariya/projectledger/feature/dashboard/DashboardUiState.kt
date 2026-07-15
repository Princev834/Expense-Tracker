package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.model.AppVariantConfiguration
import com.princevekariya.projectledger.core.model.Money

data class DashboardUiState(
    val variant: AppVariantConfiguration,
    val platformDescription: String,
    val moduleCount: Int,
    val totalBalance: Money = Money.zero(),
    val incomeThisMonth: Money = Money.zero(),
    val expensesThisMonth: Money = Money.zero(),
    val activeAccountCount: Int = 0,
    val recentTransactions: List<DashboardTransactionItem> = emptyList(),
    val loadState: UiLoadState = UiLoadState.Loading,
)
