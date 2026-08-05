package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = lightColorScheme(
    primary = SoftBluePrimary,
    onPrimary = SoftBlueOnPrimary,
    primaryContainer = SoftBluePrimaryContainer,
    onPrimaryContainer = SoftBlueOnPrimaryContainer,
    secondary = SoftBlueSecondary,
    onSecondary = SoftBlueOnSecondary,
    background = Color(0xFFF4F9FD),
    onBackground = SoftBlueOnBackground,
    surface = SoftBlueSurface,
    onSurface = SoftBlueOnSurface,
    surfaceVariant = Color(0xFFEBF5FF),
    onSurfaceVariant = SoftBlueOnSurfaceVariant,
    outline = SoftBlueOutline,
    error = ElegantError,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = SoftBluePrimary,
    onPrimary = SoftBlueOnPrimary,
    primaryContainer = SoftBluePrimaryContainer,
    onPrimaryContainer = SoftBlueOnPrimaryContainer,
    secondary = SoftBlueSecondary,
    onSecondary = SoftBlueOnSecondary,
    background = Color(0xFFF4F9FD),
    onBackground = SoftBlueOnBackground,
    surface = SoftBlueSurface,
    onSurface = SoftBlueOnSurface,
    surfaceVariant = Color(0xFFEBF5FF),
    onSurfaceVariant = SoftBlueOnSurfaceVariant,
    outline = SoftBlueOutline,
    error = ElegantError,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
