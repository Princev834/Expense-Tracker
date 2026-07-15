package com.princevekariya.projectledger.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.princevekariya.projectledger.core.common.AppLogger
import com.princevekariya.projectledger.domain.transactions.command.EpochTimeProvider
import java.time.ZoneId

class TransactionHistoryViewModelFactory(
    private val repositories: TransactionHistoryRepositories,
    private val timeProvider: EpochTimeProvider,
    private val zoneId: ZoneId,
    private val appLogger: AppLogger,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(
                TransactionHistoryViewModel::class.java,
            )
        ) {
            @Suppress("UNCHECKED_CAST")
            return TransactionHistoryViewModel(
                repositories = repositories,
                dataMapper = TransactionHistoryDataMapper(
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
