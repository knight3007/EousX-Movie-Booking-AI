package com.uit.eousx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EousDarkColorScheme = darkColorScheme(
    primary = EousPrimary,
    onPrimary = EousBackground,
    primaryContainer = EousPrimaryDark,
    onPrimaryContainer = EousBackground,

    secondary = EousSuccess,
    onSecondary = EousBackground,
    secondaryContainer = EousPanel,
    onSecondaryContainer = EousText,

    background = EousBackground,
    onBackground = EousText,

    surface = EousBackground,
    onSurface = EousText,
    surfaceVariant = EousCard,
    onSurfaceVariant = EousMuted,
    surfaceContainer = EousPanel,
    surfaceContainerHigh = EousCard,

    outline = EousBorder,
    outlineVariant = EousBorder,

    error = EousDanger,
    onError = EousText
)

@Composable
fun EousXTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EousDarkColorScheme,
        typography = Typography,
        content = content
    )
}
