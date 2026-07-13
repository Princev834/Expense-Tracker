package com.princevekariya.projectledger.feature.transactions

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
fun TransactionsPlaceholderScreen(modifier: Modifier = Modifier) {
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
            text = "Transactions",
            style = MaterialTheme.typography.headlineLarge,
        )
        LedgerSurfaceCard {
            LedgerEmptyState(
                title = "No transactions yet",
                message = "Your searchable expense and income history will appear here.",
            )
        }
    }
}
