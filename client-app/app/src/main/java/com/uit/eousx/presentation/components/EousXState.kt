package com.uit.eousx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EousXLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Loading"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(EousXSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
    ) {
        CircularProgressIndicator(color = EousXColors.PrimaryOrange)
        Text(text = message, style = EousXTypography.Body, textAlign = TextAlign.Center)
    }
}

@Composable
fun EousXEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    EousXMessageState(
        title = title,
        message = message,
        icon = icon,
        actionText = actionText,
        onActionClick = onActionClick,
        modifier = modifier
    )
}

@Composable
fun EousXErrorState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionText: String? = "Retry",
    onRetryClick: (() -> Unit)? = null
) {
    EousXMessageState(
        title = title,
        message = message,
        icon = Icons.Default.ErrorOutline,
        iconTint = EousXColors.DangerRed,
        actionText = actionText,
        onActionClick = onRetryClick,
        modifier = modifier
    )
}

@Composable
private fun EousXMessageState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Default.HourglassEmpty,
    iconTint: androidx.compose.ui.graphics.Color = EousXColors.PrimaryOrange,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(EousXSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(44.dp)
        )
        Text(text = title, style = EousXTypography.H2, textAlign = TextAlign.Center)
        if (message != null) {
            Text(text = message, style = EousXTypography.Body, textAlign = TextAlign.Center)
        }
        if (actionText != null && onActionClick != null) {
            EousXSecondaryButton(text = actionText, onClick = onActionClick)
        }
    }
}

@Preview(name = "EousX States", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXStatesPreview() {
    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.lg)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                EousXLoadingState(modifier = Modifier.weight(1f))
            }
            EousXEmptyState(
                title = "No items",
                message = "There is nothing to show yet.",
                actionText = "Refresh",
                onActionClick = {}
            )
            EousXErrorState(
                title = "Something went wrong",
                message = "Please try again.",
                onRetryClick = {}
            )
        }
    }
}
