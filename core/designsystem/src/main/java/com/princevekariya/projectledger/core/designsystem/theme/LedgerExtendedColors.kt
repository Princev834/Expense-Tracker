package com.princevekariya.projectledger.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class LedgerExtendedColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

internal val LedgerDarkExtendedColors = LedgerExtendedColors(
    income = LedgerIncome,
    onIncome = LedgerOnPrimary,
    incomeContainer = LedgerIncomeContainer,
    onIncomeContainer = LedgerOnIncomeContainer,
    expense = LedgerExpense,
    onExpense = LedgerOnError,
    expenseContainer = LedgerExpenseContainer,
    onExpenseContainer = LedgerOnExpenseContainer,
    warning = LedgerWarning,
    onWarning = LedgerBackground,
    warningContainer = LedgerWarningContainer,
    onWarningContainer = LedgerOnWarningContainer,
    info = LedgerInfo,
    onInfo = LedgerOnSecondary,
    infoContainer = LedgerInfoContainer,
    onInfoContainer = LedgerOnInfoContainer,
)

internal val LocalLedgerExtendedColors = staticCompositionLocalOf {
    LedgerDarkExtendedColors
}

val MaterialTheme.ledgerColors: LedgerExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLedgerExtendedColors.current
