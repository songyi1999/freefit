package com.freefit.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF4ADE80)
private val SkyBlue = Color(0xFF38BDF8)

private val DarkScheme = darkColorScheme(
    primary = Green,
    onPrimary = Color(0xFF052E16),
    secondary = SkyBlue,
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111A2C),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF16A34A),
    secondary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
)

@Composable
fun FreeFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}
