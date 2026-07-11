package com.princevekariya.projectledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectInitializationTest {
    @Test
    fun debugBuild_usesDedicatedApplicationIdentity() {
        assertEquals(
            "com.princevekariya.projectledger.debug",
            BuildConfig.APPLICATION_ID,
        )
        assertTrue(BuildConfig.DEBUG)
    }

    @Test
    fun debugBuild_usesDevelopmentConfiguration() {
        assertEquals("development", BuildConfig.APP_ENVIRONMENT)
        assertTrue(BuildConfig.ENABLE_VERBOSE_LOGGING)
    }
}
