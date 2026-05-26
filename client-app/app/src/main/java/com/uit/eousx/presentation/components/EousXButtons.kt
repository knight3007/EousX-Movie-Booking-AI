package com.uit.eousx.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  4. BUTTONS
// ─────────────────────────────────────────────

/**
 * 🟠 Primary Button — Orange fill with trailing arrow
 *
 * Usage:
 *   EousXPrimaryButton(text = "Book Now") { /* onClick */ }
 */
@Composable
fun EousXPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(EousXRadius.pill),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = EousXColors.PrimaryOrange,
            contentColor           = EousXColors.OnAccent,
            disabledContainerColor = EousXColors.PrimaryOrange.copy(alpha = 0.4f),
            disabledContentColor   = EousXColors.OnAccent.copy(alpha = 0.45f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Text(
            text       = text,
            style      = EousXTypography.Label,
            fontWeight = FontWeight.Bold,
            fontSize   = 15.sp
        )
        if (showArrow) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * ⬛ Secondary Button — Dark surface with border
 *
 * Usage:
 *   EousXSecondaryButton(text = "View Details") { }
 */
@Composable
fun EousXSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(EousXRadius.pill),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = EousXColors.SurfaceVariant,
            contentColor   = EousXColors.SoftIvory
        ),
        border   = BorderStroke(1.dp, EousXColors.Divider),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Text(text = text, style = EousXTypography.Label)
    }
}

/**
 * 🔶 Outline Button — Orange border, transparent fill
 *
 * Usage:
 *   EousXOutlineButton(text = "Watch Trailer") { }
 */
@Composable
fun EousXOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(EousXRadius.pill),
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = EousXColors.Transparent,
            contentColor   = EousXColors.PrimaryOrange
        ),
        border   = BorderStroke(1.5.dp, EousXColors.PrimaryOrange),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Text(text = text, style = EousXTypography.Label.copy(color = EousXColors.PrimaryOrange))
    }
}

/**
 * 🔴 Danger Button — Red fill for destructive actions
 *
 * Usage:
 *   EousXDangerButton(text = "Cancel Booking") { }
 */
@Composable
fun EousXDangerButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = modifier.height(48.dp),
        shape    = RoundedCornerShape(EousXRadius.pill),
        colors   = ButtonDefaults.buttonColors(
            containerColor = EousXColors.DangerRed,
            contentColor   = EousXColors.SoftIvory
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        Text(text = text, style = EousXTypography.Label)
    }
}

/**
 * 🩷 Icon Button — Circular icon-only button (e.g. Favourite)
 *
 * Usage:
 *   EousXIconButton(icon = Icons.Default.FavoriteBorder) { }
 */
@Composable
fun EousXIconButton(
    icon: ImageVector = Icons.Default.FavoriteBorder,
    contentDescription: String? = null,
    tint: Color = EousXColors.PrimaryOrange,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        onClick  = onClick,
        modifier = modifier
            .size(48.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = EousXColors.SurfaceVariant,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = icon,
                    contentDescription = contentDescription,
                    tint               = tint,
                    modifier           = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(name = "EousX Buttons", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXButtonsPreview() {
    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            EousXPrimaryButton(text = "Dat ve ngay", modifier = Modifier.fillMaxWidth()) {}
            EousXSecondaryButton(text = "Xem chi tiet", modifier = Modifier.fillMaxWidth()) {}
            EousXOutlineButton(text = "Xem trailer", modifier = Modifier.fillMaxWidth()) {}
            EousXDangerButton(text = "Huy dat ve", modifier = Modifier.fillMaxWidth()) {}
            EousXIconButton {}
        }
    }
}
