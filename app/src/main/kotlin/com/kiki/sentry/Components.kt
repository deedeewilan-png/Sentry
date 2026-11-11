package com.kiki.sentry

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        Status.LIVE -> "LIVE" to C.Live
        Status.CONNECTING -> "CONNECTING" to C.Warn
        Status.OFFLINE -> "OFFLINE" to C.Danger
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .alpha(if (status == Status.LIVE) pulse else 1f)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp, fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun VideoSurface(session: LiveSession, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        LiveVideo(session, Modifier.aspectRatio(session.aspect))
        if (session.status != Status.LIVE) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (session.status == Status.CONNECTING) {
                    CircularProgressIndicator(color = C.Accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Text("NO SIGNAL", color = C.Danger, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(session.error ?: "", color = C.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun HoldButton(
    icon: ImageVector,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pressed) C.Accent else C.SurfaceHi)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    onDown()
                    tryAwaitRelease()
                    pressed = false
                    onUp()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (pressed) Color.Black else C.Text)
    }
}

@Composable
fun PtzPad(onMove: (Float, Float, Float) -> Unit, onStop: () -> Unit) {
    val v = 0.5f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(C.Surface)
                .border(1.dp, C.Line, CircleShape)
        ) {
            HoldButton(Icons.Filled.KeyboardArrowUp, { onMove(0f, v, 0f) }, onStop,
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
            HoldButton(Icons.Filled.KeyboardArrowDown, { onMove(0f, -v, 0f) }, onStop,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
            HoldButton(Icons.Filled.KeyboardArrowLeft, { onMove(-v, 0f, 0f) }, onStop,
                Modifier.align(Alignment.CenterStart).padding(start = 12.dp))
            HoldButton(Icons.Filled.KeyboardArrowRight, { onMove(v, 0f, 0f) }, onStop,
                Modifier.align(Alignment.CenterEnd).padding(end = 12.dp))
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .border(2.dp, C.Accent.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(10.dp).background(C.Accent, CircleShape))
            }
        }
        Spacer(Modifier.width(20.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(C.Surface)
                .border(1.dp, C.Line, RoundedCornerShape(40.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HoldButton(Icons.Filled.Add, { onMove(0f, 0f, v) }, onStop)
            Text("ZOOM", color = C.Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            HoldButton(Icons.Filled.Remove, { onMove(0f, 0f, -v) }, onStop)
        }
    }
}
