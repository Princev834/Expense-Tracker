package com.princevekariya.projectledger

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectInitializationTest {
    @Test
    fun debugBuild_usesDedicatedApplicationIdentity() {
        assertTrue(
            BuildConfig.APPLICATION_ID.startsWith(
                "com.princevekariya.projectledger",
            ),
        )
        assertTrue(BuildConfig.APPLICATION_ID.endsWith(".debug"))
        assertTrue(BuildConfig.DEBUG)
    }

    @Test
    fun debugBuild_usesDevelopmentConfiguration() {
        assertTrue(BuildConfig.APP_ENVIRONMENT == "development")
        assertTrue(BuildConfig.ENABLE_VERBOSE_LOGGING)
    }

    @Test
    fun distributionFlags_areInternallyConsistent() {
        when (BuildConfig.DISTRIBUTION_CHANNEL) {
            "personal" -> {
                assertTrue(BuildConfig.SMS_AUTOMATION_AVAILABLE)
                assertFalse(BuildConfig.PLAY_STORE_SAFE)
            }

            "play" -> {
                assertFalse(BuildConfig.SMS_AUTOMATION_AVAILABLE)
                assertTrue(BuildConfig.PLAY_STORE_SAFE)
            }

            else -> error(
                "Unexpected distribution channel: ${BuildConfig.DISTRIBUTION_CHANNEL}",
            )
        }
    }
}
