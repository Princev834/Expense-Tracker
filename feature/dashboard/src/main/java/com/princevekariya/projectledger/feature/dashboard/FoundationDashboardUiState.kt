package com.princevekariya.projectledger.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.princevekariya.projectledger.core.designsystem.theme.ProjectLedgerTheme
import com.princevekariya.projectledger.core.model.AppDistribution
import com.princevekariya.projectledger.core.model.AppVariantConfiguration

data class FoundationDashboardUiState(
    val variant: AppVariantConfiguration,
    val platformDescription: String,
    val moduleCount: Int,
)

@Composable
fun FoundationDashboard(state: FoundationDashboardUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Project Ledger",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Code-quality foundation configured successfully.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                FoundationLine(
                    label = "Edition",
                    value = state.variant.displayName,
                )
                FoundationLine(
                    label = "Platform",
                    value = state.platformDescription,
                )
                FoundationLine(
                    label = "Gradle modules",
                    value = state.moduleCount.toString(),
                )
                FoundationLine(
                    label = "SMS capability",
                    value = if (state.variant.supportsSmsAutomation) {
                        "Personal build boundary available"
                    } else {
                        "Excluded from Play build"
                    },
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Phase 6 • code-quality foundation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FoundationLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        modifier = Modifier.padding(vertical = 3.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
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
