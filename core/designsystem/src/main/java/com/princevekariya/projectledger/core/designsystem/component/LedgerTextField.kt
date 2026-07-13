package com.princevekariya.projectledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.princevekariya.projectledger.core.designsystem.theme.ledgerSpacing

@Composable
fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    LedgerFieldContainer(
        errorMessage = errorMessage,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(text = label) },
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(text = placeholder)
                }
            },
            singleLine = true,
            isError = errorMessage != null,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
fun LedgerAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    LedgerFieldContainer(
        errorMessage = errorMessage,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(text = label) },
            leadingIcon = { Text(text = "INR") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
            ),
            singleLine = true,
            isError = errorMessage != null,
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun LedgerFieldContainer(errorMessage: String?, modifier: Modifier, content: @Composable () -> Unit) {
    val spacing = MaterialTheme.ledgerSpacing

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        content()
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
