package com.princevekariya.projectledger

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectInitializationTest {
    @Test
    fun applicationId_usesTemporaryProjectIdentity() {
        assertEquals(
            "com.princevekariya.projectledger",
            BuildConfig.APPLICATION_ID,
        )
    }
}
