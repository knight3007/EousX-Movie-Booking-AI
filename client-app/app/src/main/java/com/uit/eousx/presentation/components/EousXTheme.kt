package com.uit.eousx.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  2. COLOR PALETTE
// ─────────────────────────────────────────────

object EousXColors {
    val PrimaryOrange  = Color(0xFFFFB23F)
    val NeonLime       = Color(0xFFF1C75B)
    val Charcoal       = Color(0xFF0B0D12)
    val SoftIvory      = Color(0xFFEDE7D8)
    val SlateGray      = Color(0xFF7C8290)

    // Derived / Semantic
    val Surface        = Color(0xFF141820)
    val SurfaceVariant = Color(0xFF1E2430)
    val SurfaceRaised  = Color(0xFF252B36)
    val OnSurface      = SoftIvory
    val OnSurfaceDim   = Color(0xFFC2B9A8)
    val OnAccent       = Color(0xFF23140A)
    val DangerRed      = Color(0xFFFF5438)
    val SuccessGreen   = Color(0xFF68D07C)
    val Transparent    = Color(0x00000000)
    val OrangeAlpha12  = Color(0x1FFFB23F)
    val OrangeAlpha20  = Color(0x33FFB23F)
    val DangerAlpha12  = Color(0x1FFF5438)
    val SuccessAlpha16 = Color(0x2968D07C)
    val LimeAlpha20    = Color(0x33F1C75B)
    val Divider        = Color(0xFF343A46)
}

// ─────────────────────────────────────────────
//  3. TYPOGRAPHY
// ─────────────────────────────────────────────

// NOTE: Drop your Roboto Flex font files into res/font/ and register here.
// Fallback uses default sans-serif if not available.
val RobotoFlex = FontFamily.Default   // Replace with: FontFamily(Font(R.font.roboto_flex, ...))

object EousXTypography {
    val H1 = TextStyle(
        fontFamily  = RobotoFlex,
        fontWeight  = FontWeight.Bold,
        fontSize    = 28.sp,
        lineHeight  = 36.sp,
        color       = EousXColors.SoftIvory
    )
    val H2 = TextStyle(
        fontFamily  = RobotoFlex,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 20.sp,
        lineHeight  = 28.sp,
        color       = EousXColors.SoftIvory
    )
    val Body = TextStyle(
        fontFamily  = RobotoFlex,
        fontWeight  = FontWeight.Normal,
        fontSize    = 14.sp,
        lineHeight  = 20.sp,
        color       = EousXColors.OnSurfaceDim
    )
    val Caption = TextStyle(
        fontFamily  = RobotoFlex,
        fontWeight  = FontWeight.Medium,
        fontSize    = 12.sp,
        lineHeight  = 16.sp,
        color       = EousXColors.SlateGray
    )
    val Label = TextStyle(
        fontFamily  = RobotoFlex,
        fontWeight  = FontWeight.SemiBold,
        fontSize    = 13.sp,
        lineHeight  = 18.sp,
        color       = EousXColors.SoftIvory
    )
}

// ─────────────────────────────────────────────
//  SPACING / SHAPE TOKENS
// ─────────────────────────────────────────────

object EousXSpacing {
    val xs:  Dp = 4.dp
    val sm:  Dp = 8.dp
    val md:  Dp = 12.dp
    val lg:  Dp = 16.dp
    val xl:  Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl:Dp = 32.dp
}

object EousXRadius {
    val sm:  Dp = 8.dp
    val md:  Dp = 12.dp
    val lg:  Dp = 16.dp
    val xl:  Dp = 24.dp
    val pill:Dp = 50.dp
}

// ─────────────────────────────────────────────
//  MATERIAL THEME WRAPPER
// ─────────────────────────────────────────────

private val EousXColorScheme = darkColorScheme(
    primary         = EousXColors.PrimaryOrange,
    secondary       = EousXColors.NeonLime,
    background      = EousXColors.Charcoal,
    surface         = EousXColors.Surface,
    surfaceVariant  = EousXColors.SurfaceVariant,
    onPrimary       = EousXColors.OnAccent,
    onBackground    = EousXColors.SoftIvory,
    onSurface       = EousXColors.SoftIvory,
    error           = EousXColors.DangerRed,
    onError         = EousXColors.SoftIvory,
)

@Composable
fun EousXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EousXColorScheme,
        content     = content
    )
}
