package com.kiki.sentry

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Onvif {
    private const val WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    private const val WSU = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    private const val PWD_DIGEST = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest"
    private const val B64 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun header(user: String, pass: String): String {
        if (user.isEmpty()) return ""
        val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val created = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        val sha = MessageDigest.getInstance("SHA-1")
        sha.update(nonce)
        sha.update(created.toByteArray())
        sha.update(pass.toByteArray())
        val digest = Base64.encodeToString(sha.digest(), Base64.NO_WRAP)
        val n64 = Base64.encodeToString(nonce, Base64.NO_WRAP)
        return "<s:Header><Security xmlns=\"$WSSE\" s:mustUnderstand=\"1\"><UsernameToken>" +
            "<Username>${esc(user)}</Username>" +
            "<Password Type=\"$PWD_DIGEST\">$digest</Password>" +
            "<Nonce EncodingType=\"$B64\">$n64</Nonce>" +
            "<Created xmlns=\"$WSU\">$created</Created>" +
            "</UsernameToken></Security></s:Header>"
    }

    fun call(url: String, cam: Camera, body: String): String {
        val env = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\">" +
            header(cam.user, cam.pass) + "<s:Body>" + body + "</s:Body></s:Envelope>"
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 5000
            c.readTimeout = 6000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8")
            c.outputStream.use { it.write(env.toByteArray()) }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) {
                val reason = Regex("<(?:\\w+:)?Text[^>]*>([^<]+)").find(text)?.groupValues?.get(1)
                throw IOException("HTTP $code" + (reason?.let { ": $it" } ?: ""))
            }
            return text
        } finally {
            c.disconnect()
        }
    }

    fun xaddr(xml: String, section: String): String? =
        Regex("<(?:\\w+:)?$section>\\s*<(?:\\w+:)?XAddr>\\s*([^<\\s]+)").find(xml)?.groupValues?.get(1)

    fun profileToken(xml: String): String? =
        Regex("<(?:\\w+:)?Profiles[^>]*\\btoken=\"([^\"]+)\"").find(xml)?.groupValues?.get(1)

    const val GET_CAPS = "<tds:GetCapabilities xmlns:tds=\"http://www.onvif.org/ver10/device/wsdl\"><tds:Category>All</tds:Category></tds:GetCapabilities>"
    const val GET_PROFILES = "<trt:GetProfiles xmlns:trt=\"http://www.onvif.org/ver10/media/wsdl\"/>"
}

class PtzController(private val cam: Camera) {
    private val mutex = Mutex()
    private var ptzUrl: String? = null
    private var token: String? = null

    /** Camera reports its LAN address; swap in the host and port the user configured so it works remotely. */
    private fun fix(x: String): String {
        val u = URL(x)
        return "http://${cam.host}:${cam.onvifPort}${u.path}" + (u.query?.let { "?$it" } ?: "")
    }

    private fun ensure(): String? {
        if (ptzUrl != null && token != null) return null
        val base = "http://${cam.host}:${cam.onvifPort}"
        var media = "$base/onvif/media_service"
        var ptz = "$base/onvif/ptz_service"
        try {
            val caps = Onvif.call("$base/onvif/device_service", cam, Onvif.GET_CAPS)
            Onvif.xaddr(caps, "Media")?.let { media = fix(it) }
            val p = Onvif.xaddr(caps, "PTZ") ?: return "This camera reports no PTZ support"
            ptz = fix(p)
        } catch (e: IOException) {
            if (e.message?.startsWith("HTTP 4") == true) return explain(e)
        }
        return try {
            val profiles = try {
                Onvif.call(media, cam, Onvif.GET_PROFILES)
            } catch (e: IOException) {
                Onvif.call("$base/onvif/device_service", cam, Onvif.GET_PROFILES)
            }
            val t = Onvif.profileToken(profiles) ?: return "No ONVIF profile found"
            ptzUrl = ptz
            token = t
            null
        } catch (e: IOException) {
            explain(e)
        }
    }

    private fun explain(e: Exception): String {
        val m = e.message ?: "unknown error"
        return when {
            m.contains("401") || m.contains("Not Authorized", true) || m.contains("Sender not", true) ->
                "ONVIF login failed. Check user, password and camera clock"
            else -> "PTZ unreachable: $m"
        }
    }

    suspend fun move(x: Float, y: Float, zoom: Float): String? = mutex.withLock {
        withContext(Dispatchers.IO) {
            ensure()?.let { return@withContext it }
            val body = "<tptz:ContinuousMove xmlns:tptz=\"http://www.onvif.org/ver20/ptz/wsdl\">" +
                "<tptz:ProfileToken>$token</tptz:ProfileToken><tptz:Velocity>" +
                "<tt:PanTilt x=\"$x\" y=\"$y\" xmlns:tt=\"http://www.onvif.org/ver10/schema\"/>" +
                "<tt:Zoom x=\"$zoom\" xmlns:tt=\"http://www.onvif.org/ver10/schema\"/>" +
                "</tptz:Velocity></tptz:ContinuousMove>"
            try { Onvif.call(ptzUrl!!, cam, body); null } catch (e: IOException) { explain(e) }
        }
    }

    suspend fun stop() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (ptzUrl == null || token == null) return@withContext
            val body = "<tptz:Stop xmlns:tptz=\"http://www.onvif.org/ver20/ptz/wsdl\">" +
                "<tptz:ProfileToken>$token</tptz:ProfileToken>" +
                "<tptz:PanTilt>true</tptz:PanTilt><tptz:Zoom>true</tptz:Zoom></tptz:Stop>"
            try { Onvif.call(ptzUrl!!, cam, body) } catch (_: IOException) {}
        }
    }
}
