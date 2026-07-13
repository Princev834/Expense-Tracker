package com.princevekariya.projectledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.princevekariya.projectledger.config.CurrentAppVariant
import com.princevekariya.projectledger.core.designsystem.theme.ProjectLedgerTheme
import com.princevekariya.projectledger.feature.dashboard.FoundationDashboardUiState
import com.princevekariya.projectledger.navigation.ProjectLedgerApp
import com.princevekariya.projectledger.platform.device.AndroidDeviceInfoProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceInfo = AndroidDeviceInfoProvider().getDeviceInfo()

        setContent {
            ProjectLedgerTheme {
                ProjectLedgerApp(
                    dashboardState = FoundationDashboardUiState(
                        variant = CurrentAppVariant.configuration,
                        platformDescription = deviceInfo.displayValue,
                        moduleCount = PROJECT_MODULE_COUNT,
                    ),
                )
            }
        }
    }

    private companion object {
        const val PROJECT_MODULE_COUNT: Int = 8
    }
}
