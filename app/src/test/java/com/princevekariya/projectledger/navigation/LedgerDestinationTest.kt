package com.princevekariya.projectledger.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerDestinationTest {
    @Test
    fun routesAreUnique() {
        val routes = LedgerDestination.values().map(LedgerDestination::route)

        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun deepLinksUseThePrivateApplicationScheme() {
        val deepLinks = LedgerDestination.values().map(LedgerDestination::deepLinkUri)

        assertTrue(deepLinks.all { it.startsWith("projectledger://") })
    }
}
