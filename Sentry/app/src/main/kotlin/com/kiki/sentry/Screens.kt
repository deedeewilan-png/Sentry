package com.kiki.sentry

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

// ---------------------------------------------------------------- HOME

@Composable
fun HomeScreen(
    cameras: List<Camera>,
    onOpen: (Camera) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Camera) -> Unit
) {
    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("SENTRY", color = C.Text, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
                    Text(
                        if (cameras.size == 1) "1 CAMERA" else "${cameras.size} CAMERAS",
                        color = C.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
                    )
                }
                Lens(30.dp)
            }
            if (cameras.isEmpty()) {
                EmptyState(onAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cameras, key = { it.id }) { cam ->
                        CameraTile(cam, onOpen = { onOpen(cam) }, onEdit = { onEdit(cam) })
                    }
                }
            }
        }
        if (cameras.isNotEmpty()) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 22.dp)
                    .clip(RoundedCornerShape(50))
                    .background(C.Accent)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("ADD CAMERA", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun Lens(size: androidx.compose.ui.unit.Dp) {
    val glow by rememberInfiniteTransition(label = "lens").animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g"
    )
    Box(Modifier.size(size).border(1.5.dp, C.Line, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 0.62f).border(2.dp, C.Accent.copy(alpha = glow), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * 0.24f).background(C.Accent, CircleShape))
        }
    }
}

@Composable
fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Lens(120.dp)
        Spacer(Modifier.height(28.dp))
        Text("No cameras yet", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add an RTSP or ONVIF camera to watch it live and control it from anywhere.",
            color = C.Muted, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(C.Accent)
                .clickable(onClick = onAdd)
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text("ADD CAMERA", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp)
        }
    }
}

@Composable
fun CameraTile(camera: Camera, onOpen: () -> Unit, onEdit: () -> Unit) {
    val session = rememberSession(camera, startMuted = true)
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .border(1.dp, C.Line, shape)
            .clickable(onClick = onOpen)
    ) {
        VideoSurface(session, Modifier.fillMaxSize(), compact = true)
        Box(
            Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
        )
        StatusPill(session.status, Modifier.align(Alignment.TopStart).padding(12.dp))
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Edit, null, tint = C.Text, modifier = Modifier.size(16.dp)) }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(camera.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(camera.host, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ---------------------------------------------------------------- VIEWER

@Composable
fun ActionButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(C.Surface)
            .border(1.dp, C.Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = C.Text)
        Spacer(Modifier.height(6.dp))
        Text(label, color = C.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
    }
}

@Composable
fun ViewerScreen(camera: Camera, onBack: () -> Unit, onEdit: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()
    val session = rememberSession(camera, startMuted = false)
    val ptz = remember(camera.id, camera.host, camera.onvifPort, camera.user, camera.pass) { PtzController(camera) }
    val scope = rememberCoroutineScope()
    var fullscreen by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var ptzError by remember { mutableStateOf<String?>(null) }

    BackHandler { if (fullscreen) fullscreen = false else onBack() }

    DisposableEffect(fullscreen) {
        val w = activity?.window
        if (activity != null && w != null) {
            val ctl = WindowCompat.getInsetsController(w, w.decorView)
            if (fullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                ctl.hide(WindowInsetsCompat.Type.systemBars())
                ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ctl.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            val w = activity?.window
            w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (w != null) WindowCompat.getInsetsController(w, w.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }
    LaunchedEffect(note) {
        if (note != null) {
            delay(2200)
            note = null
        }
    }

    val onMove: (Float, Float, Float) -> Unit = { x, y, z -> scope.launch { ptzError = ptz.move(x, y, z) } }
    val onStop: () -> Unit = { scope.launch { ptz.stop() } }
    val snapshot: () -> Unit = {
        val ok = saveSnapshot(ctx, session.texture, camera.name)
        note = if (ok) "Saved to Pictures/Sentry" else "No frame to capture yet"
    }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoSurface(session, Modifier.fillMaxSize())
            StatusPill(session.status, Modifier.align(Alignment.TopStart).padding(16.dp))
            Row(Modifier.align(Alignment.BottomEnd).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundIcon(Icons.Filled.PhotoCamera, snapshot)
                RoundIcon(if (session.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp) { session.toggleMute() }
                RoundIcon(Icons.Filled.FullscreenExit) { fullscreen = false }
            }
            Note(note, Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
        }
        return
    }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(camera.name, color = C.Text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(camera.host, color = C.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                StatusPill(session.status)
                Spacer(Modifier.width(10.dp))
                RoundIcon(Icons.Filled.Edit, onEdit)
            }
            Box(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, C.Line, RoundedCornerShape(22.dp))
            ) {
                VideoSurface(session, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(Icons.Filled.PhotoCamera, "SNAP", Modifier.weight(1f), snapshot)
                ActionButton(
                    if (session.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    if (session.muted) "UNMUTE" else "MUTE", Modifier.weight(1f)
                ) { session.toggleMute() }
                ActionButton(Icons.Filled.Refresh, "RECONNECT", Modifier.weight(1f)) { session.connect() }
                ActionButton(Icons.Filled.Fullscreen, "FULL", Modifier.weight(1f)) { fullscreen = true }
            }
            Spacer(Modifier.height(26.dp))
            if (camera.ptz) {
                Text(
                    "PAN  TILT  ZOOM", color = C.Muted, fontSize = 11.sp, letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PtzPad(onMove, onStop) }
                ptzError?.let {
                    Text(it, color = C.Danger, fontSize = 12.sp, modifier = Modifier.padding(20.dp).align(Alignment.CenterHorizontally))
                }
            } else {
                Text(
                    "PTZ is off for this camera. Turn it on in Edit if your camera can pan and tilt.",
                    color = C.Muted, fontSize = 13.sp, modifier = Modifier.padding(24.dp)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
        Note(note, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp))
    }
}

@Composable
fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, C.Line, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = C.Text, modifier = Modifier.size(20.dp)) }
}

@Composable
fun Note(text: String?, modifier: Modifier = Modifier) {
    if (text == null) return
    Text(
        text, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(50)).background(C.Accent).padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------- ADD / EDIT

@Composable
fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.Accent,
            unfocusedBorderColor = C.Line,
            focusedLabelColor = C.Accent,
            unfocusedLabelColor = C.Muted,
            cursorColor = C.Accent,
            focusedContainerColor = C.Surface,
            unfocusedContainerColor = C.Surface,
            focusedTextColor = C.Text,
            unfocusedTextColor = C.Text
        )
    )
}

@Composable
fun EditScreen(
    initial: Camera?,
    onBack: () -> Unit,
    onSave: (Camera) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var host by remember { mutableStateOf(initial?.host ?: "") }
    var rtspPort by remember { mutableStateOf((initial?.rtspPort ?: 554).toString()) }
    var path by remember { mutableStateOf(initial?.path ?: "/stream1") }
    var user by remember { mutableStateOf(initial?.user ?: "") }
    var pass by remember { mutableStateOf(initial?.pass ?: "") }
    var onvifPort by remember { mutableStateOf((initial?.onvifPort ?: 80).toString()) }
    var ptz by remember { mutableStateOf(initial?.ptz ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val cleanHost = host.trim().replace(Regex("^\\w+://"), "").substringBefore("/")
    val canSave = name.isNotBlank() && cleanHost.isNotBlank()

    Column(
        Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
            Spacer(Modifier.width(14.dp))
            Text(if (initial == null) "New camera" else "Edit camera", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("BRAND PRESET", color = C.Muted, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { p ->
                Text(
                    p.name, color = C.Text, fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(C.SurfaceHi)
                        .border(1.dp, C.Line, RoundedCornerShape(50))
                        .clickable {
                            rtspPort = p.rtspPort.toString()
                            path = p.path
                            onvifPort = p.onvifPort.toString()
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Field("Name", name, { name = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Field("Address (IP, DDNS or Tailscale)", host, { host = it }, Modifier.fillMaxWidth(), KeyboardType.Uri)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("RTSP port", rtspPort, { rtspPort = it }, Modifier.weight(1f), KeyboardType.Number)
            Field("ONVIF port", onvifPort, { onvifPort = it }, Modifier.weight(1f), KeyboardType.Number)
        }
        Spacer(Modifier.height(12.dp))
        Field("Stream path", path, { path = it }, Modifier.fillMaxWidth(), KeyboardType.Uri)
        Spacer(Modifier.height(12.dp))
        Field("Username", user, { user = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Field("Password", pass, { pass = it }, Modifier.fillMaxWidth(), password = true)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pan / tilt / zoom", color = C.Text, fontWeight = FontWeight.SemiBold)
                Text("Show the PTZ pad (needs ONVIF)", color = C.Muted, fontSize = 12.sp)
            }
            Switch(
                checked = ptz, onCheckedChange = { ptz = it },
                colors = SwitchDefaults.colors(checkedTrackColor = C.Accent, checkedThumbColor = Color.Black)
            )
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(C.Accent)
                .alpha(if (canSave) 1f else 0.35f)
                .clickable(enabled = canSave) {
                    onSave(
                        Camera(
                            id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            host = cleanHost,
                            rtspPort = rtspPort.toIntOrNull() ?: 554,
                            path = path.trim(),
                            user = user.trim(),
                            pass = pass,
                            onvifPort = onvifPort.toIntOrNull() ?: 80,
                            ptz = ptz
                        )
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("SAVE AND CONNECT", color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp)
        }
        if (onDelete != null) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete camera", color = C.Danger)
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = C.Surface,
            title = { Text("Delete this camera?", color = C.Text) },
            text = { Text("It will be removed from this phone. The camera itself is not changed.", color = C.Muted) },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete", color = C.Danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = C.Text) } }
        )
    }
}
