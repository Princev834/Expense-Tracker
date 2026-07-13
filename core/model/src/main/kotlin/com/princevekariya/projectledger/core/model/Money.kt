package com.princevekariya.projectledger.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode = CurrencyCode.INR,
) {
    val isZero: Boolean
        get() = minorUnits == 0L

    val isPositive: Boolean
        get() = minorUnits > 0L

    val isNegative: Boolean
        get() = minorUnits < 0L

    operator fun plus(other: Money): Money {
        requireMatchingCurrency(other)
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }

    operator fun minus(other: Money): Money {
        requireMatchingCurrency(other)
        return copy(minorUnits = Math.subtractExact(minorUnits, other.minorUnits))
    }

    operator fun unaryMinus(): Money = copy(
        minorUnits = Math.negateExact(minorUnits),
    )

    fun absoluteValue(): Money = when {
        isNegative -> -this
        else -> this
    }

    fun formatted(): String {
        val decimalValue = BigDecimal.valueOf(
            minorUnits,
            currency.minorUnitScale,
        )
        val formatter = DecimalFormat(
            decimalPattern(scale = currency.minorUnitScale),
            DecimalFormatSymbols(Locale.ENGLISH),
        ).apply {
            roundingMode = RoundingMode.UNNECESSARY
            isParseBigDecimal = true
        }
        return "${currency.isoCode} ${formatter.format(decimalValue)}"
    }

    private fun requireMatchingCurrency(other: Money) {
        require(currency == other.currency) {
            "Money values must use the same currency."
        }
    }

    companion object {
        fun zero(currency: CurrencyCode = CurrencyCode.INR): Money = Money(
            minorUnits = 0L,
            currency = currency,
        )

        fun fromMajorUnits(rawValue: String, currency: CurrencyCode = CurrencyCode.INR): Money {
            val normalizedValue = rawValue
                .trim()
                .replace(",", "")
            require(normalizedValue.isNotBlank()) {
                "Money input cannot be blank."
            }

            val decimalValue = normalizedValue.toBigDecimal()
                .setScale(
                    currency.minorUnitScale,
                    RoundingMode.UNNECESSARY,
                )
            val minorUnits = decimalValue
                .movePointRight(currency.minorUnitScale)
                .longValueExact()

            return Money(
                minorUnits = minorUnits,
                currency = currency,
            )
        }

        fun parseMajorUnits(rawValue: String, currency: CurrencyCode = CurrencyCode.INR): Result<Money> = runCatching {
            fromMajorUnits(
                rawValue = rawValue,
                currency = currency,
            )
        }

        private fun decimalPattern(scale: Int): String = when {
            scale <= 0 -> "#,##0"
            else -> "#,##0.${"0".repeat(scale)}"
        }
    }
}
