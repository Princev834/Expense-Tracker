package com.princevekariya.projectledger.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import java.time.ZoneId

class MonthlyReportViewModelFactory(
    private val repositories: MonthlyReportRepositories,
    private val timeProvider: EpochTimeProvider,
    private val zoneId: ZoneId,
    private val appLogger: AppLogger,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(
                MonthlyReportViewModel::class.java,
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return MonthlyReportViewModel(
                repositories = repositories,
                dataMapper = MonthlyReportDataMapper(
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
