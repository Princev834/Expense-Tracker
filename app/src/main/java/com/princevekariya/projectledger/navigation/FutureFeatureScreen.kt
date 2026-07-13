package com.princevekariya.projectledger.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.princevekariya.projectledger.core.designsystem.component.LedgerEmptyState
import com.princevekariya.projectledger.core.designsystem.component.LedgerSurfaceCard
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
internal fun FutureFeatureScreen(title: String, message: String, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = spacing.screenHorizontal,
                vertical = spacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
        )
        LedgerSurfaceCard {
            LedgerEmptyState(
                title = "$title foundation ready",
                message = message,
            )
        }
    }
}
