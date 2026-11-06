package com.opankcs.costa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CostaAmber,
    onPrimary = CostaAmberInk,
    secondary = CostaOk,
    onSecondary = CostaGraphite,
    background = CostaGraphite,
    onBackground = CostaInk,
    surface = CostaPanel,
    onSurface = CostaInk,
    surfaceVariant = CostaPanel2,
    onSurfaceVariant = CostaMuted,
    outline = CostaLine,
    error = CostaDanger,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB37700),
    onPrimary = Color.White,
    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF18181B),
    surface = Color.White,
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF1F1EE),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFD4D4D8),
    error = Color(0xFFDC2626),
)

@Composable
fun CostaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
