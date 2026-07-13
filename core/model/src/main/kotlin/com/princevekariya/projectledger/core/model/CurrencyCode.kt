package com.princevekariya.projectledger.core.model

enum class CurrencyCode(
    val isoCode: String,
    val minorUnitScale: Int,
) {
    INR(
        isoCode = "INR",
        minorUnitScale = 2,
    ),
}
