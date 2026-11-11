package com.kiki.sentry

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object C {
    val Bg = Color(0xFF070A14)
    val Surface = Color(0xCC101526)
    val SurfaceHi = Color(0xFF1A2036)
    val Line = Color(0xFF2A3350)
    val Accent = Color(0xFF2DE2E6)
    val Accent2 = Color(0xFF8B5CF6)
    val Live = Color(0xFF3DFFA2)
    val Danger = Color(0xFFFF3D81)
    val Warn = Color(0xFFFFB020)
    val Text = Color(0xFFF2F5FF)
    val Muted = Color(0xFF8E98B8)
    val Grad = Brush.horizontalGradient(listOf(Accent, Accent2))
}

@Composable
fun Modifier.appBackground(): Modifier {
    val t by rememberInfiniteTransition(label = "bg").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t"
    )
    return this.background(C.Bg).drawBehind {
        val w = size.width
        val h = size.height
        drawRect(Brush.radialGradient(listOf(C.Accent2.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(w * (0.15f + 0.5f * t), h * 0.10f), radius = w * 0.95f))
        drawRect(Brush.radialGradient(listOf(C.Accent.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(w * (0.9f - 0.5f * t), h * 0.88f), radius = w * 0.95f))
    }
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
