package com.princevekariya.projectledger.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.princevekariya.projectledger.feature.dashboard.FoundationDashboardUiState

@Composable
fun ProjectLedgerApp(dashboardState: FoundationDashboardUiState, modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ProjectLedgerBottomBar(navController = navController)
        },
    ) { contentPadding ->
        ProjectLedgerNavHost(
            navController = navController,
            dashboardState = dashboardState,
            contentPadding = contentPadding,
        )
    }
}
