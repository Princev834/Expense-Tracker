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
import com.princevekariya.projectledger.core.model.TransactionType
import com.princevekariya.projectledger.feature.dashboard.DashboardRoute
import com.princevekariya.projectledger.feature.dashboard.DashboardViewModelFactory
import com.princevekariya.projectledger.feature.reports.MonthlyReportRoute
import com.princevekariya.projectledger.feature.reports.MonthlyReportViewModelFactory
import com.princevekariya.projectledger.feature.settings.AccountSettingsRoute
import com.princevekariya.projectledger.feature.settings.AccountSettingsViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionEntryRoute
import com.princevekariya.projectledger.feature.transactions.TransactionEntryViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryRoute
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryViewModelFactory

@Composable
internal fun ProjectLedgerNavHost(
    navController: NavHostController,
    dashboardViewModelFactory: DashboardViewModelFactory,
    transactionEntryViewModelFactory: TransactionEntryViewModelFactory,
    transactionHistoryViewModelFactory: TransactionHistoryViewModelFactory,
    monthlyReportViewModelFactory: MonthlyReportViewModelFactory,
    accountSettingsViewModelFactory: AccountSettingsViewModelFactory,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = LedgerDestination.HOME.route,
        modifier = Modifier.padding(contentPadding),
    ) {
        homeDestination(
            navController = navController,
            factory = dashboardViewModelFactory,
        )
        transactionsDestination(
            factory = transactionHistoryViewModelFactory,
        )
        reportsDestination(
            factory = monthlyReportViewModelFactory,
        )
        settingsDestination(
            factory = accountSettingsViewModelFactory,
        )
        transactionEntryDestination(
            navController = navController,
            factory = transactionEntryViewModelFactory,
        )
    }
}

private fun NavGraphBuilder.homeDestination(navController: NavHostController, factory: DashboardViewModelFactory) {
    composable(
        route = LedgerDestination.HOME.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = LedgerDestination.HOME.deepLinkUri
            },
        ),
    ) {
        DashboardRoute(
            factory = factory,
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

private fun NavGraphBuilder.transactionsDestination(factory: TransactionHistoryViewModelFactory) {
    composable(
        route = LedgerDestination.TRANSACTIONS.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = LedgerDestination.TRANSACTIONS.deepLinkUri
            },
        ),
    ) {
        TransactionHistoryRoute(factory = factory)
    }
}

private fun NavGraphBuilder.reportsDestination(factory: MonthlyReportViewModelFactory) {
    composable(
        route = LedgerDestination.REPORTS.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = LedgerDestination.REPORTS.deepLinkUri
            },
        ),
    ) {
        MonthlyReportRoute(factory = factory)
    }
}

private fun NavGraphBuilder.settingsDestination(factory: AccountSettingsViewModelFactory) {
    composable(
        route = LedgerDestination.SETTINGS.route,
        deepLinks = listOf(
            navDeepLink {
                uriPattern = LedgerDestination.SETTINGS.deepLinkUri
            },
        ),
    ) {
        AccountSettingsRoute(factory = factory)
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
