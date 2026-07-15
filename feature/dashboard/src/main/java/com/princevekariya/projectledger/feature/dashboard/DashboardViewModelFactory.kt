package com.princevekariya.projectledger.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import java.time.ZoneId

class DashboardViewModelFactory(
    private val initialState: DashboardUiState,
    private val repositories: DashboardRepositories,
    private val timeProvider: EpochTimeProvider,
    private val zoneId: ZoneId,
    private val appLogger: AppLogger,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                initialState = initialState,
                repositories = repositories,
                dataMapper = DashboardDataMapper(
                    timeProvider = timeProvider,
                    zoneId = zoneId,
                ),
                appLogger = appLogger,
            ) as T
        }

        throw IllegalArgumentException(
            "Unsupported ViewModel class: ${modelClass.name}",
        )
    }
}
