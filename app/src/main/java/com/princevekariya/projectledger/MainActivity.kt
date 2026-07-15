package com.princevekariya.projectledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.princevekariya.projectledger.config.CurrentAppVariant
import com.princevekariya.projectledger.core.common.info
import com.princevekariya.projectledger.core.designsystem.theme.ProjectLedgerTheme
import com.princevekariya.projectledger.di.AppContainer
import com.princevekariya.projectledger.feature.dashboard.DashboardUiState
import com.princevekariya.projectledger.navigation.ProjectLedgerApp
import com.princevekariya.projectledger.platform.device.AndroidDeviceInfoProvider

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer
        get() = (application as ProjectLedgerApplication).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appContainer.appLogger.info(
            event = "main_activity_created",
            message = "Main application activity resolved its dependencies.",
        )
        val deviceInfo = AndroidDeviceInfoProvider().getDeviceInfo()
        val initialDashboardState = DashboardUiState(
            variant = CurrentAppVariant.configuration,
            platformDescription = deviceInfo.displayValue,
            moduleCount = PROJECT_MODULE_COUNT,
        )
        val dashboardViewModelFactory =
            appContainer.createDashboardViewModelFactory(
                initialState = initialDashboardState,
            )

        setContent {
            ProjectLedgerTheme {
                ProjectLedgerApp(
                    dashboardViewModelFactory = dashboardViewModelFactory,
                    transactionEntryViewModelFactory =
                    appContainer.transactionEntryViewModelFactory,
                    transactionHistoryViewModelFactory =
                    appContainer.transactionHistoryViewModelFactory,
                    monthlyReportViewModelFactory =
                    appContainer.monthlyReportViewModelFactory,
                    accountSettingsViewModelFactory =
                    appContainer.accountSettingsViewModelFactory,
                )
            }
        }
    }

    private companion object {
        const val PROJECT_MODULE_COUNT: Int = 11
    }
}
