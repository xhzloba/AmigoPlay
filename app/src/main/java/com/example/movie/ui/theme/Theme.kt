package com.example.movie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinemaColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = CinemaMuted,
    background = CinemaBlack,
    surface = CinemaSurface,
    onBackground = CinemaText,
    onSurface = CinemaText,
    onSurfaceVariant = CinemaMuted,
    surfaceVariant = Color(0xFF1C1C24),
    outline = Color(0x33FFFFFF),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun MovieTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = Typography,
        content = content,
    )
}
