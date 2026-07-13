package com.princevekariya.projectledger.feature.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
fun DashboardRoute(initialState: DashboardUiState, appLogger: AppLogger, modifier: Modifier = Modifier) {
    val factory = remember(initialState, appLogger) {
        DashboardViewModelFactory(
            initialState = initialState,
            appLogger = appLogger,
        )
    }
    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val state by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage = state.userMessage
    val spacing = MaterialTheme.ledgerSpacing

    LaunchedEffect(userMessage?.id) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(message = userMessage.text)
            dashboardViewModel.onAction(
                DashboardAction.MessageShown(id = userMessage.id),
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        FoundationDashboard(
            state = state,
            onAction = dashboardViewModel::onAction,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(spacing.medium),
        )
    }
}
