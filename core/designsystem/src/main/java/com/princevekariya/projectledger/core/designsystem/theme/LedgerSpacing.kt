package com.princevekariya.projectledger.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class LedgerSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 20.dp,
    val section: Dp = 24.dp,
    val spacious: Dp = 32.dp,
    val expanded: Dp = 40.dp,
    val screenHorizontal: Dp = 20.dp,
    val screenVertical: Dp = 24.dp,
)

internal val DefaultLedgerSpacing = LedgerSpacing()

internal val LocalLedgerSpacing = staticCompositionLocalOf {
    DefaultLedgerSpacing
}

val MaterialTheme.ledgerSpacing: LedgerSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalLedgerSpacing.current
