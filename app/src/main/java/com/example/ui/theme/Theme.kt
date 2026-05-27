package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PolishLightColorScheme = lightColorScheme(
    primary = AccentNeonBlue,
    secondary = AccentPurpleGlow,
    tertiary = AccentSunsetPink,
    background = SlateDark,
    surface = SlateCard,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateCharcoal,
    onSurfaceVariant = SlateTextSecondary
)

@Composable
fun TictokAdvertTheme(
    darkTheme: Boolean = false, // Set false to apply the polished light theme colors
    content: @Composable () -> Unit,
) {
    val colorScheme = PolishLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

