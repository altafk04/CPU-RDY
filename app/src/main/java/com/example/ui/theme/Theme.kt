package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AzureBlueLight,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = TechCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF004F5C),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = VmwareGreen,
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF00522F),
    onTertiaryContainer = Color(0xFF86FBB3),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkSurfaceBorder,
    error = StatusCritical,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AzureBluePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = AzureBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7E7FF),
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = VmwareGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FADF),
    onTertiaryContainer = Color(0xFF002110),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightSurfaceBorder,
    error = StatusCritical,
    onError = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
