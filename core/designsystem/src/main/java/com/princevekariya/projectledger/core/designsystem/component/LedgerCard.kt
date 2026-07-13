package com.princevekariya.projectledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.princevekariya.projectledger.core.designsystem.theme.LedgerElevation
import com.princevekariya.projectledger.core.designsystem.theme.ledgerColors
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
fun LedgerSurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = LedgerElevation.card,
        ),
    ) {
        Column(
            modifier = Modifier.padding(spacing.section),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            content = content,
        )
    }
}

@Composable
fun LedgerMetricCard(
    title: String,
    value: String,
    supportingText: String,
    tone: LedgerMetricTone,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.ledgerSpacing
    val accent = metricToneColor(tone)

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun metricToneColor(tone: LedgerMetricTone): Color {
    val ledgerColors = MaterialTheme.ledgerColors

    return when (tone) {
        LedgerMetricTone.NEUTRAL -> MaterialTheme.colorScheme.primary
        LedgerMetricTone.INCOME -> ledgerColors.income
        LedgerMetricTone.EXPENSE -> ledgerColors.expense
        LedgerMetricTone.WARNING -> ledgerColors.warning
    }
}
