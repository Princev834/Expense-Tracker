package com.princevekariya.projectledger.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.princevekariya.projectledger.feature.dashboard.FoundationDashboard
import com.princevekariya.projectledger.feature.dashboard.FoundationDashboardUiState
import com.princevekariya.projectledger.feature.transactions.TransactionsPlaceholderScreen

@Composable
internal fun ProjectLedgerNavHost(
    navController: NavHostController,
    dashboardState: FoundationDashboardUiState,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = LedgerDestination.HOME.route,
        modifier = Modifier.padding(contentPadding),
    ) {
        composable(
            route = LedgerDestination.HOME.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = LedgerDestination.HOME.deepLinkUri
                },
            ),
        ) {
            FoundationDashboard(state = dashboardState)
        }

        composable(
            route = LedgerDestination.TRANSACTIONS.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = LedgerDestination.TRANSACTIONS.deepLinkUri
                },
            ),
        ) {
            TransactionsPlaceholderScreen()
        }

        composable(
            route = LedgerDestination.REPORTS.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = LedgerDestination.REPORTS.deepLinkUri
                },
            ),
        ) {
            FutureFeatureScreen(
                title = "Reports",
                message = "Monthly charts, summaries, and PDF export will be built here.",
            )
        }

        composable(
            route = LedgerDestination.SETTINGS.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = LedgerDestination.SETTINGS.deepLinkUri
                },
            ),
        ) {
            FutureFeatureScreen(
                title = "Settings",
                message = "Accounts, categories, reminders, security, and sync controls will live here.",
            )
        }
    }
}
