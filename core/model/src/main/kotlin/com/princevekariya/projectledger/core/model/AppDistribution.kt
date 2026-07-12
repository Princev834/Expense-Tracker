package com.princevekariya.projectledger.core.model

enum class AppDistribution {
    PERSONAL,
    PLAY,
}

data class AppVariantConfiguration(
    val distribution: AppDistribution,
    val displayName: String,
    val supportsSmsAutomation: Boolean,
    val isPlayStoreSafe: Boolean,
)
