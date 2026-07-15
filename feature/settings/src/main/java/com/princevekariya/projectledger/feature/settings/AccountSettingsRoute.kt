package com.princevekariya.projectledger.feature.settings

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
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
fun AccountSettingsRoute(factory: AccountSettingsViewModelFactory, modifier: Modifier = Modifier) {
    val accountSettingsViewModel: AccountSettingsViewModel = viewModel(
        factory = factory,
    )
    val state by accountSettingsViewModel.uiState
        .collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.userMessage
    val spacing = MaterialTheme.ledgerSpacing

    LaunchedEffect(message?.id) {
        if (message != null) {
            snackbarHostState.showSnackbar(message = message.text)
            accountSettingsViewModel.onAction(
                AccountSettingsAction.MessageShown(id = message.id),
            )
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AccountSettingsScreen(
            state = state,
            onAction = accountSettingsViewModel::onAction,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(spacing.medium),
        )
    }
}
