package com.princevekariya.projectledger.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.princevekariya.projectledger.R

enum class LedgerDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val deepLinkUri: String,
) {
    HOME(
        route = "home",
        labelRes = R.string.navigation_home,
        iconRes = R.drawable.ic_navigation_home,
        deepLinkUri = "projectledger://home",
    ),
    TRANSACTIONS(
        route = "transactions",
        labelRes = R.string.navigation_transactions,
        iconRes = R.drawable.ic_navigation_transactions,
        deepLinkUri = "projectledger://transactions",
    ),
    REPORTS(
        route = "reports",
        labelRes = R.string.navigation_reports,
        iconRes = R.drawable.ic_navigation_reports,
        deepLinkUri = "projectledger://reports",
    ),
    SETTINGS(
        route = "settings",
        labelRes = R.string.navigation_settings,
        iconRes = R.drawable.ic_navigation_settings,
        deepLinkUri = "projectledger://settings",
    ),
}
