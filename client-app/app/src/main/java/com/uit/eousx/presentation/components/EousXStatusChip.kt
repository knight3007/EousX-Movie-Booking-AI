package com.uit.eousx.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  6. STATUS CHIPS
// ─────────────────────────────────────────────

/** All possible chip states defined in the UI kit */
enum class EousXChipStatus {
    NOW_SHOWING,
    UPCOMING,
    PAID,
    LOCKED,
    SOLD,
    AVAILABLE
}

private data class ChipStyle(
    val label:      String,
    val containerColor: Color,
    val contentColor:   Color,
    val borderColor:    Color,
    val showLock:   Boolean = false,
)

private fun chipStyleFor(status: EousXChipStatus): ChipStyle = when (status) {
    EousXChipStatus.NOW_SHOWING -> ChipStyle(
        label          = "ĐANG CHIẾU",
        containerColor = EousXColors.OrangeAlpha20,
        contentColor   = EousXColors.PrimaryOrange,
        borderColor    = EousXColors.PrimaryOrange
    )
    EousXChipStatus.UPCOMING    -> ChipStyle(
        label          = "SẮP CHIẾU",
        containerColor = EousXColors.Transparent,
        contentColor   = EousXColors.OnSurfaceDim,
        borderColor    = EousXColors.Divider
    )
    EousXChipStatus.PAID        -> ChipStyle(
        label          = "ĐÃ THANH TOÁN",
        containerColor = EousXColors.SuccessAlpha16,
        contentColor   = EousXColors.SuccessGreen,
        borderColor    = EousXColors.SuccessGreen
    )
    EousXChipStatus.LOCKED      -> ChipStyle(
        label          = "ĐÃ KHÓA",
        containerColor = EousXColors.Transparent,
        contentColor   = EousXColors.OnSurfaceDim,
        borderColor    = EousXColors.Divider,
        showLock       = true
    )
    EousXChipStatus.SOLD        -> ChipStyle(
        label          = "HẾT VÉ",
        containerColor = EousXColors.DangerAlpha12,
        contentColor   = EousXColors.DangerRed,
        borderColor    = EousXColors.DangerRed
    )
    EousXChipStatus.AVAILABLE   -> ChipStyle(
        label          = "CÒN VÉ",
        containerColor = EousXColors.Transparent,
        contentColor   = EousXColors.OnSurfaceDim,
        borderColor    = EousXColors.Divider
    )
}

/**
 * 🏷️ Status Chip — reflects one of 6 states from the design system
 *
 * Usage:
 *   EousXStatusChip(status = EousXChipStatus.NOW_SHOWING)
 *   EousXStatusChip(status = EousXChipStatus.SOLD)
 *
 * @param status    The movie/ticket status to display
 * @param modifier  Optional modifier
 * @param onClick   Optional — makes the chip clickable/selectable
 */
@Composable
fun EousXStatusChip(
    status: EousXChipStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val style = chipStyleFor(status)
    val shape = RoundedCornerShape(EousXRadius.pill)

    Surface(
        modifier  = modifier,
        shape     = shape,
        color     = style.containerColor,
        border    = BorderStroke(1.dp, style.borderColor),
        onClick   = onClick ?: {},
        enabled   = onClick != null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (style.showLock) {
                Icon(
                    imageVector        = Icons.Default.Lock,
                    contentDescription = null,
                    tint               = style.contentColor,
                    modifier           = Modifier.size(12.dp)
                )
            }
            Text(
                text  = style.label,
                style = EousXTypography.Caption.copy(
                    color      = style.contentColor,
                    fontSize   = 11.sp,
                    letterSpacing = 0.5.sp
                ),
            )
        }
    }
}

/**
 * Convenience alias — pass a raw string label if you have a custom state.
 *
 * Usage:
 *   EousXCustomChip(label = "PRE-SALE", contentColor = Color.Yellow, borderColor = Color.Yellow)
 */
@Composable
fun EousXCustomChip(
    label: String,
    containerColor: Color = EousXColors.Transparent,
    contentColor: Color   = EousXColors.OnSurfaceDim,
    borderColor: Color    = EousXColors.Divider,
    modifier: Modifier    = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(EousXRadius.pill)
    Surface(
        modifier  = modifier,
        shape     = shape,
        color     = containerColor,
        border    = BorderStroke(1.dp, borderColor),
        onClick   = onClick ?: {},
        enabled   = onClick != null,
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style    = EousXTypography.Caption.copy(
                color       = contentColor,
                fontSize    = 11.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Preview(name = "EousX Status Chips", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXStatusChipsPreview() {
    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.sm)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm)) {
                EousXStatusChip(EousXChipStatus.NOW_SHOWING)
                EousXStatusChip(EousXChipStatus.UPCOMING)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm)) {
                EousXStatusChip(EousXChipStatus.PAID)
                EousXStatusChip(EousXChipStatus.LOCKED)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm)) {
                EousXStatusChip(EousXChipStatus.SOLD)
                EousXStatusChip(EousXChipStatus.AVAILABLE)
            }
        }
    }
}
