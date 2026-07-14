package com.princevekariya.projectledger.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.feature.dashboard.DashboardRoute
import com.princevekariya.projectledger.feature.dashboard.DashboardUiState
import com.princevekariya.projectledger.feature.transactions.TransactionEntryRoute
import com.princevekariya.projectledger.feature.transactions.TransactionEntryViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionsPlaceholderScreen

@Composable
internal fun ProjectLedgerNavHost(
    navController: NavHostController,
    dashboardInitialState: DashboardUiState,
    transactionEntryViewModelFactory: TransactionEntryViewModelFactory,
    appLogger: AppLogger,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = LedgerDestination.HOME.route,
        modifier = Modifier.padding(contentPadding),
    ) {
        homeDestination(
            navController = navController,
            dashboardInitialState = dashboardInitialState,
            appLogger = appLogger,
        )
        transactionsDestination()
        reportsDestination()
        settingsDestination()
        transactionEntryDestination(
            navController = navController,
            factory = transactionEntryViewModelFactory,
        )
    }
}

private fun NavGraphBuilder.homeDestination(
    navController: NavHostController,
    dashboardInitialState: DashboardUiState,
    appLogger: AppLogger,
) {
    composable(
        route = LedgerDestination.HOME.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = LedgerDestination.HOME.deepLinkUri
            },
        ),
    ) {
        DashboardRoute(
            initialState = dashboardInitialState,
            appLogger = appLogger,
            onAddExpense = {
                navController.navigateToTransactionEntry(
                    type = TransactionType.EXPENSE,
                )
            },
            onAddIncome = {
                navController.navigateToTransactionEntry(
                    type = TransactionType.INCOME,
                )
            },
        )
    }
}

private fun NavGraphBuilder.transactionsDestination() {
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
}

private fun NavGraphBuilder.reportsDestination() {
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
}

private fun NavGraphBuilder.settingsDestination() {
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

private fun NavGraphBuilder.transactionEntryDestination(
    navController: NavHostController,
    factory: TransactionEntryViewModelFactory,
) {
    composable(
        route = TransactionEntryDestination.ROUTE_PATTERN,
        arguments = listOf(
            navArgument(
                name = TransactionEntryDestination.TRANSACTION_TYPE_ARGUMENT,
            ) {
                type = NavType.StringType
            },
        ),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = TransactionEntryDestination.DEEP_LINK_PATTERN
            },
        ),
    ) { backStackEntry ->
        val initialType = TransactionEntryDestination.parseType(
            value = backStackEntry.arguments?.getString(
                TransactionEntryDestination.TRANSACTION_TYPE_ARGUMENT,
            ),
        )

        TransactionEntryRoute(
            factory = factory,
            initialTransactionType = initialType,
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}

private fun NavHostController.navigateToTransactionEntry(type: TransactionType) {
    navigate(
        route = TransactionEntryDestination.createRoute(type = type),
    ) {
        launchSingleTop = true
    }
}
