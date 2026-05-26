package com.uit.eousx.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class SeatStatus {
    AVAILABLE,
    SELECTED,
    LOCKED,
    SOLD,
    MAINTENANCE
}

data class SeatUiState(
    val id: String,
    val label: String,
    val status: SeatStatus,
    val enabled: Boolean = status == SeatStatus.AVAILABLE || status == SeatStatus.SELECTED
)

@Composable
fun EousXSeat(
    seat: SeatUiState,
    onClick: (SeatUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = seatStyle(seat.status)
    Box(
        modifier = modifier
            .size(42.dp)
            .background(style.containerColor, RoundedCornerShape(EousXRadius.sm))
            .border(1.dp, style.borderColor, RoundedCornerShape(EousXRadius.sm))
            .clickable(enabled = seat.enabled) { onClick(seat) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seat.label,
            style = EousXTypography.Caption.copy(color = style.contentColor),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun EousXSeatLegend(
    modifier: Modifier = Modifier,
    statuses: List<SeatStatus> = SeatStatus.entries
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EousXSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEach { status ->
            val style = seatStyle(status)
            Row(
                horizontalArrangement = Arrangement.spacedBy(EousXSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(style.containerColor, RoundedCornerShape(4.dp))
                        .border(1.dp, style.borderColor, RoundedCornerShape(4.dp))
                )
                Text(text = status.label(), style = EousXTypography.Caption)
            }
        }
    }
}

@Composable
fun EousXScreenIndicator(
    modifier: Modifier = Modifier,
    label: String = "Screen"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(EousXSpacing.xs)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .size(height = 18.dp, width = 1.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(EousXColors.Transparent, EousXColors.PrimaryOrange, EousXColors.Transparent)
                ),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 5.dp.toPx()
            )
        }
        Text(text = label, style = EousXTypography.Caption.copy(color = EousXColors.PrimaryOrange))
    }
}

private data class SeatStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color
)

private fun seatStyle(status: SeatStatus): SeatStyle = when (status) {
    SeatStatus.AVAILABLE -> SeatStyle(EousXColors.SurfaceVariant, EousXColors.SoftIvory, EousXColors.Divider)
    SeatStatus.SELECTED -> SeatStyle(EousXColors.PrimaryOrange, EousXColors.OnAccent, EousXColors.PrimaryOrange)
    SeatStatus.LOCKED -> SeatStyle(EousXColors.Transparent, EousXColors.SlateGray, EousXColors.Divider)
    SeatStatus.SOLD -> SeatStyle(EousXColors.DangerAlpha12, EousXColors.DangerRed, EousXColors.DangerRed)
    SeatStatus.MAINTENANCE -> SeatStyle(EousXColors.LimeAlpha20, EousXColors.NeonLime, EousXColors.NeonLime)
}

private fun SeatStatus.label(): String = when (this) {
    SeatStatus.AVAILABLE -> "Available"
    SeatStatus.SELECTED -> "Selected"
    SeatStatus.LOCKED -> "Locked"
    SeatStatus.SOLD -> "Sold"
    SeatStatus.MAINTENANCE -> "Maintenance"
}

@Preview(name = "EousX Seats", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXSeatsPreview() {
    EousXTheme {
        Surface(color = EousXColors.Charcoal) {
            Column(
                modifier = Modifier.padding(EousXSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(EousXSpacing.lg)
            ) {
                EousXScreenIndicator()
                Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm)) {
                    SeatStatus.entries.forEachIndexed { index, status ->
                        EousXSeat(
                            seat = SeatUiState(
                                id = status.name,
                                label = "A${index + 1}",
                                status = status
                            ),
                            onClick = {}
                        )
                    }
                }
                EousXSeatLegend()
            }
        }
    }
}
