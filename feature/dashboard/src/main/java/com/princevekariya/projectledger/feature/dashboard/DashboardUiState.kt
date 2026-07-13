package com.princevekariya.projectledger.feature.dashboard

import com.princevekariya.projectledger.core.common.UiLoadState
import com.princevekariya.projectledger.core.common.UiMessage
import com.princevekariya.projectledger.core.model.AppVariantConfiguration

data class DashboardUiState(
    val variant: AppVariantConfiguration,
    val platformDescription: String,
    val moduleCount: Int,
    val description: String = "Lunch at college",
    val amount: String = "120",
    val loadState: UiLoadState = UiLoadState.Content,
    val userMessage: UiMessage? = null,
)
