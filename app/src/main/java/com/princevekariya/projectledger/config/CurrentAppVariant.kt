package com.princevekariya.projectledger.config

import com.princevekariya.projectledger.BuildConfig
import com.princevekariya.projectledger.core.model.AppDistribution
import com.princevekariya.projectledger.core.model.AppVariantConfiguration

object CurrentAppVariant {
    val configuration: AppVariantConfiguration by lazy {
        val distribution = when (BuildConfig.DISTRIBUTION_CHANNEL) {
            "personal" -> AppDistribution.PERSONAL
            "play" -> AppDistribution.PLAY
            else -> error(
                "Unsupported distribution channel: ${BuildConfig.DISTRIBUTION_CHANNEL}",
            )
        }

        AppVariantConfiguration(
            distribution = distribution,
            displayName = when (distribution) {
                AppDistribution.PERSONAL -> "Personal APK"
                AppDistribution.PLAY -> "Play Store"
            },
            supportsSmsAutomation = BuildConfig.SMS_AUTOMATION_AVAILABLE,
            isPlayStoreSafe = BuildConfig.PLAY_STORE_SAFE,
        )
    }
}
