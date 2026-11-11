package com.kiki.sentry

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object C {
    val Bg = Color(0xFF0B0D10)
    val Surface = Color(0xFF14181D)
    val SurfaceHi = Color(0xFF1D242C)
    val Line = Color(0xFF28313B)
    val Accent = Color(0xFFC6FF3D)
    val Danger = Color(0xFFFF4D5E)
    val Warn = Color(0xFFFFB020)
    val Text = Color(0xFFF2F5F7)
    val Muted = Color(0xFF8A96A3)
}

@Composable
fun SentryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Accent,
            onPrimary = Color.Black,
            background = C.Bg,
            onBackground = C.Text,
            surface = C.Surface,
            onSurface = C.Text,
            surfaceVariant = C.SurfaceHi,
            onSurfaceVariant = C.Muted,
            outline = C.Line,
            error = C.Danger
        ),
        content = content
    )
}
