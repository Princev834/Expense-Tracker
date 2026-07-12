package com.princevekariya.projectledger.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.princevekariya.projectledger.core.designsystem.theme.LedgerElevation
import com.princevekariya.projectledger.core.designsystem.theme.ProjectLedgerTheme
import com.princevekariya.projectledger.core.designsystem.theme.ledgerColors
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing
import com.princevekariya.projectledger.core.model.AppDistribution
import com.princevekariya.projectledger.core.model.AppVariantConfiguration

data class FoundationDashboardUiState(
    val variant: AppVariantConfiguration,
    val platformDescription: String,
    val moduleCount: Int,
)

@Composable
fun FoundationDashboard(state: FoundationDashboardUiState, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.ledgerSpacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = spacing.screenHorizontal,
                vertical = spacing.screenVertical,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
        ) {
            DashboardHeader()
            FoundationDetailsCard(state = state)
            SemanticTokenRow()
        }
    }
}

@Composable
private fun DashboardHeader() {
    val spacing = MaterialTheme.ledgerSpacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
        Text(
            text = "Project Ledger",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "A consistent dark finance design system is now active.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoundationDetailsCard(state: FoundationDashboardUiState) {
    val spacing = MaterialTheme.ledgerSpacing

    Card(
        modifier = Modifier.fillMaxWidth(),
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
        ) {
            Text(
                text = "Design-token foundation configured successfully.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            FoundationLine(label = "Edition", value = state.variant.displayName)
            FoundationLine(label = "Platform", value = state.platformDescription)
            FoundationLine(label = "Gradle modules", value = state.moduleCount.toString())
            FoundationLine(
                label = "SMS capability",
                value = if (state.variant.supportsSmsAutomation) {
                    "Personal build boundary available"
                } else {
                    "Excluded from Play build"
                },
            )
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            Text(
                text = "Phase 8 • design-token foundation",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun FoundationLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SemanticTokenRow() {
    val spacing = MaterialTheme.ledgerSpacing
    val colors = MaterialTheme.ledgerColors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        SemanticToken(
            label = "Income",
            color = colors.income,
            modifier = Modifier.weight(1f),
        )
        SemanticToken(
            label = "Expense",
            color = colors.expense,
            modifier = Modifier.weight(1f),
        )
        SemanticToken(
            label = "Warning",
            color = colors.warning,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SemanticToken(label: String, color: Color, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.ledgerSpacing

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.medium,
                vertical = spacing.small,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.small)
                    .background(color = color, shape = CircleShape),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF080C11)
@Composable
@Suppress("UnusedPrivateMember")
private fun FoundationDashboardPreview() {
    ProjectLedgerTheme {
        FoundationDashboard(
            state = FoundationDashboardUiState(
                variant = AppVariantConfiguration(
                    distribution = AppDistribution.PERSONAL,
                    displayName = "Personal APK",
                    supportsSmsAutomation = true,
                    isPlayStoreSafe = false,
                ),
                platformDescription = "Android 14 (API 34)",
                moduleCount = 8,
            ),
        )
    }
}
