package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = ElectricNeonViolet,
    secondary = CyberCyan,
    tertiary = NeonGreen,
    background = CyberBlack,
    surface = CyberBlack,
    surfaceContainer = CyberDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary,
    outline = CyberPanelBorder
)

@Composable
fun CyberChartTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
