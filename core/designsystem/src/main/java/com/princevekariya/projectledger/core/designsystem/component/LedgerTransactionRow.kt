package com.princevekariya.projectledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.princevekariya.projectledger.core.designsystem.theme.ledgerColors
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
fun LedgerTransactionRow(
    title: String,
    subtitle: String,
    amount: String,
    direction: LedgerTransactionDirection,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.ledgerSpacing
    val accent = transactionDirectionColor(direction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.extraLarge)
                .background(
                    color = accent.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.small)
                    .background(color = accent, shape = CircleShape),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun transactionDirectionColor(direction: LedgerTransactionDirection): Color {
    val ledgerColors = MaterialTheme.ledgerColors

    return when (direction) {
        LedgerTransactionDirection.INCOME -> ledgerColors.income
        LedgerTransactionDirection.EXPENSE -> ledgerColors.expense
        LedgerTransactionDirection.TRANSFER -> ledgerColors.info
    }
}
