package com.princevekariya.projectledger.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.princevekariya.projectledger.feature.dashboard.DashboardViewModelFactory
import com.princevekariya.projectledger.feature.reports.MonthlyReportViewModelFactory
import com.princevekariya.projectledger.feature.settings.AccountSettingsViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionEntryViewModelFactory
import com.princevekariya.projectledger.feature.transactions.TransactionHistoryViewModelFactory

@Composable
fun ProjectLedgerApp(
    dashboardViewModelFactory: DashboardViewModelFactory,
    transactionEntryViewModelFactory: TransactionEntryViewModelFactory,
    transactionHistoryViewModelFactory: TransactionHistoryViewModelFactory,
    monthlyReportViewModelFactory: MonthlyReportViewModelFactory,
    accountSettingsViewModelFactory: AccountSettingsViewModelFactory,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar =
        currentRoute != TransactionEntryDestination.ROUTE_PATTERN

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                ProjectLedgerBottomBar(navController = navController)
            }
        },
    ) { contentPadding ->
        ProjectLedgerNavHost(
            navController = navController,
            dashboardViewModelFactory = dashboardViewModelFactory,
            transactionEntryViewModelFactory =
            transactionEntryViewModelFactory,
            transactionHistoryViewModelFactory =
            transactionHistoryViewModelFactory,
            monthlyReportViewModelFactory =
            monthlyReportViewModelFactory,
            accountSettingsViewModelFactory =
            accountSettingsViewModelFactory,
            contentPadding = contentPadding,
        )
    }
}
