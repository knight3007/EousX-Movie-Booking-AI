package com.uit.eousx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EousXConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = EousXColors.PrimaryOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = EousXColors.OnSurfaceDim)
            }
        },
        title = { Text(title, style = EousXTypography.H2) },
        text = { Text(message, style = EousXTypography.Body) },
        containerColor = EousXColors.SurfaceVariant,
        titleContentColor = EousXColors.SoftIvory,
        textContentColor = EousXColors.OnSurfaceDim,
        modifier = modifier
    )
}

@Composable
fun EousXResultDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Done",
    icon: ImageVector = Icons.Default.CheckCircle
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = EousXColors.PrimaryOrange)
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EousXColors.SuccessGreen
            )
        },
        title = { Text(title, style = EousXTypography.H2) },
        text = { Text(message, style = EousXTypography.Body) },
        containerColor = EousXColors.SurfaceVariant,
        titleContentColor = EousXColors.SoftIvory,
        textContentColor = EousXColors.OnSurfaceDim,
        modifier = modifier
    )
}

@Preview(name = "EousX Confirm Dialog", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXConfirmDialogPreview() {
    EousXTheme {
        EousXConfirmDialog(
            title = "Confirm action",
            message = "This action needs your confirmation.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "EousX Result Dialog", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXResultDialogPreview() {
    EousXTheme {
        EousXResultDialog(
            title = "Completed",
            message = "The action finished successfully.",
            onConfirm = {}
        )
    }
}
