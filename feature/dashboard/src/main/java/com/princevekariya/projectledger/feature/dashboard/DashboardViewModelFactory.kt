package com.princevekariya.projectledger.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DashboardViewModelFactory(
    private val initialState: DashboardUiState,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(initialState = initialState) as T
        }

        throw IllegalArgumentException(
            "Unsupported ViewModel class: ${modelClass.name}",
        )
    }
}
