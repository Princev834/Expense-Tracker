package com.princevekariya.projectledger.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MonthlyReportRoute(factory: MonthlyReportViewModelFactory, modifier: Modifier = Modifier) {
    val monthlyReportViewModel: MonthlyReportViewModel = viewModel(
        factory = factory,
    )
    val state by monthlyReportViewModel.uiState
        .collectAsStateWithLifecycle()

    MonthlyReportScreen(
        state = state,
        onAction = monthlyReportViewModel::onAction,
        modifier = modifier,
    )
}
