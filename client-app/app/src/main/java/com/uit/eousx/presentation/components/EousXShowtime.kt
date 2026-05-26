package com.uit.eousx.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class ShowtimeUiState(
    val id: String,
    val time: String,
    val roomName: String,
    val roomType: String,
    val basePrice: String,
    val availableSeats: Int,
    val enabled: Boolean = true
)

@Composable
fun EousXDateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    val containerColor = if (selected) EousXColors.PrimaryOrange else EousXColors.Transparent
    val contentColor = if (selected) EousXColors.OnAccent else EousXColors.SoftIvory
    val borderColor = if (selected) EousXColors.PrimaryOrange else EousXColors.Divider

    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(EousXRadius.lg),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = EousXSpacing.lg, vertical = EousXSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = EousXTypography.Label.copy(color = contentColor))
            if (supportingText != null) {
                Text(text = supportingText, style = EousXTypography.Caption.copy(color = contentColor.copy(alpha = 0.8f)))
            }
        }
    }
}

@Composable
fun EousXShowtimeCard(
    showtime: ShowtimeUiState,
    onClick: (ShowtimeUiState) -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val borderColor = if (selected) EousXColors.PrimaryOrange else EousXColors.Divider
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = showtime.enabled) { onClick(showtime) },
        shape = RoundedCornerShape(EousXRadius.lg),
        color = EousXColors.SurfaceVariant,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (selected) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = EousXColors.PrimaryOrange)
                    Spacer(Modifier.width(EousXSpacing.sm))
                    Text(text = showtime.time, style = EousXTypography.H2)
                }
                EousXCustomChip(
                    label = showtime.roomType,
                    contentColor = EousXColors.NeonLime,
                    borderColor = EousXColors.NeonLime,
                    containerColor = EousXColors.LimeAlpha20
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = showtime.roomName, style = EousXTypography.Label)
                    Text(text = showtime.basePrice, style = EousXTypography.Caption.copy(color = EousXColors.PrimaryOrange))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventSeat, contentDescription = null, tint = EousXColors.OnSurfaceDim)
                    Spacer(Modifier.width(EousXSpacing.xs))
                    Text(
                        text = "${showtime.availableSeats} seats",
                        style = EousXTypography.Caption.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Preview(name = "EousX Showtime", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXShowtimePreview() {
    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.lg)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm)) {
                EousXDateChip(label = "Mon", supportingText = "25", selected = true, onClick = {})
                EousXDateChip(label = "Tue", supportingText = "26", selected = false, onClick = {})
            }
            EousXShowtimeCard(
                showtime = ShowtimeUiState(
                    id = "st-1",
                    time = "19:30",
                    roomName = "Room 01",
                    roomType = "IMAX",
                    basePrice = "$12.00",
                    availableSeats = 48
                ),
                selected = true,
                onClick = {}
            )
        }
    }
}
