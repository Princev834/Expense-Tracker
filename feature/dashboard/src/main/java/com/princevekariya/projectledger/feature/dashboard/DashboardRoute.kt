package com.princevekariya.projectledger.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardRoute(
    factory: DashboardViewModelFactory,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = factory,
    )
    val state by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    FoundationDashboard(
        state = state,
        onAction = dashboardViewModel::onAction,
        onAddExpense = onAddExpense,
        onAddIncome = onAddIncome,
        modifier = modifier,
    )
}
