package com.princevekariya.projectledger.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerPrimary,
    onPrimary = LedgerOnPrimary,
    primaryContainer = LedgerPrimaryContainer,
    onPrimaryContainer = LedgerOnPrimaryContainer,
    secondary = LedgerSecondary,
    onSecondary = LedgerOnSecondary,
    secondaryContainer = LedgerSecondaryContainer,
    onSecondaryContainer = LedgerOnSecondaryContainer,
    tertiary = LedgerTertiary,
    onTertiary = LedgerOnTertiary,
    tertiaryContainer = LedgerTertiaryContainer,
    onTertiaryContainer = LedgerOnTertiaryContainer,
    background = LedgerBackground,
    onBackground = LedgerOnBackground,
    surface = LedgerSurface,
    onSurface = LedgerOnBackground,
    surfaceVariant = LedgerSurfaceVariant,
    onSurfaceVariant = LedgerOnSurfaceVariant,
    error = LedgerError,
    onError = LedgerOnError,
    errorContainer = LedgerErrorContainer,
    onErrorContainer = LedgerOnErrorContainer,
    outline = LedgerOutline,
    outlineVariant = LedgerOutlineVariant,
    inverseSurface = LedgerInverseSurface,
    inverseOnSurface = LedgerInverseOnSurface,
    inversePrimary = LedgerPrimaryContainer,
    scrim = LedgerScrim,
)

@Composable
fun ProjectLedgerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLedgerExtendedColors provides LedgerDarkExtendedColors,
        LocalLedgerSpacing provides DefaultLedgerSpacing,
    ) {
        MaterialTheme(
            colorScheme = LedgerDarkColorScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content,
        )
    }
}
