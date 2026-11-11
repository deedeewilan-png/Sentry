@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.kiki.sentry

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Status { CONNECTING, LIVE, OFFLINE }

class LiveSession(context: Context, val camera: Camera, startMuted: Boolean) {
    var status by mutableStateOf(Status.CONNECTING)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var aspect by mutableStateOf(16f / 9f)
        private set
    var muted by mutableStateOf(startMuted)
        private set
    var texture: TextureView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var released = false

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 2000, 250, 500)
                .build()
        )
        .build()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                status = Status.LIVE
                error = null
            } else if (state == Player.STATE_BUFFERING && status != Status.LIVE) {
                status = Status.CONNECTING
            }
        }

        override fun onRenderedFirstFrame() {
            status = Status.LIVE
        }

        override fun onVideoSizeChanged(v: VideoSize) {
            if (v.height > 0) aspect = v.width * v.pixelWidthHeightRatio / v.height
        }

        override fun onPlayerError(e: PlaybackException) {
            status = Status.OFFLINE
            val m = (e.cause?.message ?: e.message ?: e.errorCodeName)
            error = when {
                m.contains("401") || m.contains("Unauthorized", true) -> "Wrong username or password"
                m.contains("timeout", true) || m.contains("timed out", true) ||
                    m.contains("connect", true) -> "Cannot reach camera. Check address, port and forwarding"
                m.contains("404") -> "Stream path not found. Try another preset"
                else -> m
            }
            handler.postDelayed({ if (!released) connect() }, 4000)
        }
    }

    init {
        player.volume = if (startMuted) 0f else 1f
        player.addListener(listener)
        connect()
    }

    fun connect() {
        if (released) return
        status = Status.CONNECTING
        val src = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setTimeoutMs(10000)
            .createMediaSource(MediaItem.fromUri(camera.rtspUrl()))
        player.setMediaSource(src)
        player.prepare()
        player.playWhenReady = true
    }

    fun toggleMute() {
        muted = !muted
        player.volume = if (muted) 0f else 1f
    }

    fun release() {
        released = true
        handler.removeCallbacksAndMessages(null)
        player.release()
    }
}

@Composable
fun rememberSession(camera: Camera, startMuted: Boolean): LiveSession {
    val ctx = LocalContext.current.applicationContext
    val s = remember(camera.id, camera.rtspUrl()) { LiveSession(ctx, camera, startMuted) }
    DisposableEffect(s) { onDispose { s.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { s.player.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (s.status != Status.LIVE) s.connect() else s.player.play() }
    return s
}

@Composable
fun LiveVideo(session: LiveSession, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also {
                session.player.setVideoTextureView(it)
                session.texture = it
            }
        },
        onRelease = { session.player.clearVideoTextureView(it) }
    )
}

fun saveSnapshot(ctx: Context, tv: TextureView?, name: String): Boolean {
    val bmp: Bitmap = tv?.bitmap ?: return false
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "Sentry_${name.replace(' ', '_')}_$stamp.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Sentry")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    return true
}
