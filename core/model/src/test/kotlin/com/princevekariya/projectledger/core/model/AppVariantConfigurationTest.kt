package com.princevekariya.projectledger.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVariantConfigurationTest {
    @Test
    fun personalConfiguration_canExposePrivateAutomationCapability() {
        val configuration = AppVariantConfiguration(
            distribution = AppDistribution.PERSONAL,
            displayName = "Personal APK",
            supportsSmsAutomation = true,
            isPlayStoreSafe = false,
        )

        assertTrue(configuration.supportsSmsAutomation)
        assertFalse(configuration.isPlayStoreSafe)
    }
}
