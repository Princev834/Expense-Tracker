package com.princevekariya.projectledger.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerGreen,
    background = LedgerBackground,
    surface = LedgerSurface,
    surfaceVariant = LedgerSurfaceVariant,
    onPrimary = LedgerBackground,
    onBackground = LedgerOnBackground,
    onSurface = LedgerOnBackground,
    onSurfaceVariant = LedgerOnSurfaceVariant,
    error = LedgerError,
)

@Composable
fun ProjectLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LedgerDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
