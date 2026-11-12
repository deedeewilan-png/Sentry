package com.kiki.sentry

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.PatternMatcher
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.resume

data class Found(val host: String, val port: Int, val kind: String, val detail: String, val path: String? = null)

object Finder {
    val HOTSPOT_PORTS = listOf(554, 8554, 10554, 555, 80, 81, 8080, 8081, 8000, 8070, 8090, 8899, 7070, 5000, 9000, 443, 34567)
    val SUBNET_PORTS = listOf(554, 8554, 10554, 555, 8000, 8080, 80)
    private val PATHS = listOf(
        "/", "/live/ch0", "/live/ch00_0", "/stream1", "/stream0", "/11", "/12", "/h264", "/ch0_0.h264",
        "/onvif1", "/user=admin&password=&channel=1&stream=0.sdp?", "/Streaming/Channels/101",
        "/cam/realmonitor?channel=1&subtype=0", "/h264Preview_01_main"
    )

    private fun isOpen(host: String, port: Int, timeout: Int): Boolean {
        val s = Socket()
        return try {
            s.connect(InetSocketAddress(host, port), timeout)
            true
        } catch (e: Exception) {
            false
        } finally {
            runCatching { s.close() }
        }
    }

    private fun talk(host: String, port: Int, request: String, maxLines: Int = 12): List<String> {
        val s = Socket()
        return try {
            s.connect(InetSocketAddress(host, port), 1500)
            s.soTimeout = 1500
            s.getOutputStream().apply { write(request.toByteArray()); flush() }
            val r = s.getInputStream().bufferedReader()
            val lines = ArrayList<String>()
            while (lines.size < maxLines) {
                val l = r.readLine() ?: break
                if (l.isEmpty()) break
                lines.add(l)
            }
            lines
        } catch (e: Exception) {
            emptyList()
        } finally {
            runCatching { s.close() }
        }
    }

    private fun rtsp(host: String, port: Int): Found? {
        var sawRtsp = false
        for (p in PATHS) {
            val head = talk(host, port, "DESCRIBE rtsp://$host:$port$p RTSP/1.0\r\nCSeq: 1\r\nAccept: application/sdp\r\n\r\n", 1)
            val line = head.firstOrNull() ?: break
            if (!line.startsWith("RTSP/")) return null
            sawRtsp = true
            if (line.contains(" 200")) return Found(host, port, "RTSP", "Stream open", p)
            if (line.contains(" 401")) return Found(host, port, "RTSP", "Stream found, needs login", p)
        }
        return if (sawRtsp) Found(host, port, "RTSP", "RTSP server, stream path unknown", "/") else null
    }

    private fun http(host: String, port: Int): Found? {
        val head = talk(host, port, "GET / HTTP/1.0\r\n\r\n")
        val first = head.firstOrNull() ?: return null
        if (!first.startsWith("HTTP/")) return null
        val server = head.firstOrNull { it.startsWith("Server:", true) }?.substringAfter(':')?.trim()
        return Found(host, port, "HTTP", first + (server?.let { "  ($it)" } ?: ""))
    }

    private fun probe(host: String, port: Int): Found {
        rtsp(host, port)?.let { return it }
        http(host, port)?.let { return it }
        return Found(host, port, "OPEN", "Port open")
    }

    fun scanHost(host: String, ports: List<Int>, timeout: Int): List<Found> =
        ports.mapNotNull { p -> if (isOpen(host, p, timeout)) probe(host, p) else null }

    suspend fun scanSubnet(base: String, onProgress: (Int) -> Unit, onFound: (Found) -> Unit) = coroutineScope {
        val sem = Semaphore(40)
        var done = 0
        (1..254).map { i ->
            async(Dispatchers.IO) {
                val r = sem.withPermit { scanHost("$base.$i", SUBNET_PORTS, 500) }
                withContext(Dispatchers.Main) {
                    r.forEach(onFound)
                    done++
                    onProgress(done)
                }
            }
        }.awaitAll()
    }

    fun phoneBase(ctx: Context): String? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val lp = cm.getLinkProperties(cm.activeNetwork) ?: return null
        val a = lp.linkAddresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress ?: return null
        return a.substringBeforeLast('.')
    }
}

object Hotspot {
    private var cb: ConnectivityManager.NetworkCallback? = null

    suspend fun join(ctx: Context, prefix: String, pass: String): Network? = suspendCancellableCoroutine { cont ->
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        leave(ctx)
        val b = WifiNetworkSpecifier.Builder().setSsidPattern(PatternMatcher(prefix, PatternMatcher.PATTERN_PREFIX))
        if (pass.isNotBlank()) b.setWpa2Passphrase(pass)
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(b.build())
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cm.bindProcessToNetwork(network)
                if (cont.isActive) cont.resume(network)
            }

            override fun onUnavailable() {
                if (cont.isActive) cont.resume(null)
            }
        }
        cb = callback
        cm.requestNetwork(req, callback, 40000)
        cont.invokeOnCancellation { leave(ctx) }
    }

    fun leave(ctx: Context) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        cm.bindProcessToNetwork(null)
        cb?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        cb = null
    }

    fun gateway(ctx: Context, n: Network): String? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val lp = cm.getLinkProperties(n) ?: return null
        lp.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress?.let { return it }
        val ip = lp.linkAddresses.firstOrNull { it.address is Inet4Address }?.address?.hostAddress ?: return null
        return ip.substringBeforeLast('.') + ".1"
    }
}

@Composable
private fun GradButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(C.Grad)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, fontSize = 13.sp)
    }
}

@Composable
private fun FoundCard(f: Found, onUse: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface)
            .border(1.dp, C.Line, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${f.kind}  ${f.host}:${f.port}", color = C.Text, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text(f.detail + (f.path?.let { "  $it" } ?: ""), color = C.Muted, fontSize = 12.sp)
        }
        if (f.kind == "RTSP") {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(C.Grad).clickable(onClick = onUse)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) { Text("USE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp) }
        }
    }
}

@Composable
fun FinderScreen(onBack: () -> Unit, onUse: (Found) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateListOf<Found>() }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }
    var prefix by remember { mutableStateOf("FTY") }
    var pass by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)
    DisposableEffect(Unit) { onDispose { Hotspot.leave(ctx) } }

    Column(
        Modifier.fillMaxSize().appBackground().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
            Spacer(Modifier.width(14.dp))
            Text("Find camera", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(22.dp))
        Text("1  CAMERA HOTSPOT", color = C.Muted, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text("Power the camera, then join its own Wi-Fi (the name starts with FTY on most). Sentry joins it and looks for an open stream.", color = C.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Field("Hotspot name starts with", prefix, { prefix = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Field("Hotspot password (leave empty if none)", pass, { pass = it }, Modifier.fillMaxWidth(), password = true)
        Spacer(Modifier.height(12.dp))
        GradButton("JOIN HOTSPOT AND SCAN", !busy && prefix.isNotBlank()) {
            scope.launch {
                busy = true
                results.clear()
                scanned = false
                status = "Waiting for Android to join $prefix. Accept the prompt if it appears."
                val net = Hotspot.join(ctx, prefix.trim(), pass)
                if (net == null) {
                    status = "Could not join. Check the camera hotspot is on and the name is right."
                    busy = false
                    return@launch
                }
                status = "Joined. Scanning the camera..."
                delay(1200)
                val gw = Hotspot.gateway(ctx, net)
                if (gw == null) {
                    status = "Joined, but no camera address was found."
                    busy = false
                    return@launch
                }
                val found = withContext(Dispatchers.IO) { Finder.scanHost(gw, Finder.HOTSPOT_PORTS, 700) }
                results.addAll(found)
                status = "Scan finished at $gw"
                scanned = true
                busy = false
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("2  YOUR WI-FI", color = C.Muted, fontSize = 11.sp, letterSpacing = 2.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text("After the camera has joined your router (also after a reset), scan your network to find it again.", color = C.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        GradButton("SCAN MY WI-FI", !busy) {
            scope.launch {
                Hotspot.leave(ctx)
                val base = Finder.phoneBase(ctx)
                if (base == null) {
                    status = "Connect your phone to Wi-Fi first."
                    return@launch
                }
                busy = true
                results.clear()
                scanned = false
                status = "Scanning $base.1 to $base.254"
                Finder.scanSubnet(base, { status = "Scanning $base.x   $it / 254" }, { results.add(it) })
                status = "Scan finished"
                scanned = true
                busy = false
            }
        }
        Spacer(Modifier.height(20.dp))
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = C.Accent, strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(status, color = C.Text, fontSize = 13.sp)
            }
        } else if (status.isNotEmpty()) {
            Text(status, color = C.Text, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            results.forEach { f -> FoundCard(f) { onUse(f) } }
        }
        if (scanned && results.none { it.kind == "RTSP" }) {
            Spacer(Modifier.height(12.dp))
            Text(
                "No open video stream found. This camera probably streams only through its own P2P cloud. Send me the lines above and I will plan the next step.",
                color = C.Warn, fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(30.dp))
    }
}
