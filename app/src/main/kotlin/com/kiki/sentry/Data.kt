package com.kiki.sentry

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Camera(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val rtspPort: Int = 554,
    val path: String = "/stream1",
    val user: String = "",
    val pass: String = "",
    val onvifPort: Int = 80,
    val ptz: Boolean = true
) {
    fun rtspUrl(): String {
        val cred = if (user.isNotEmpty()) "${Uri.encode(user)}:${Uri.encode(pass)}@" else ""
        val p = if (path.startsWith("/")) path else "/$path"
        return "rtsp://$cred$host:$rtspPort$p"
    }
}

data class Preset(val name: String, val rtspPort: Int, val path: String, val onvifPort: Int)

val PRESETS = listOf(
    Preset("Hikvision", 554, "/Streaming/Channels/101", 80),
    Preset("Dahua", 554, "/cam/realmonitor?channel=1&subtype=0", 80),
    Preset("Reolink", 554, "/h264Preview_01_main", 8000),
    Preset("Tapo / TP-Link", 554, "/stream1", 2020),
    Preset("Generic", 554, "/stream1", 80)
)

class CameraStore(context: Context) {
    private val prefs = context.getSharedPreferences("sentry", Context.MODE_PRIVATE)

    fun load(): List<Camera> {
        val raw = prefs.getString("cameras", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Camera(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    host = o.getString("host"),
                    rtspPort = o.optInt("rtspPort", 554),
                    path = o.optString("path", "/stream1"),
                    user = o.optString("user"),
                    pass = o.optString("pass"),
                    onvifPort = o.optInt("onvifPort", 80),
                    ptz = o.optBoolean("ptz", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Camera>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("host", c.host)
                put("rtspPort", c.rtspPort); put("path", c.path)
                put("user", c.user); put("pass", c.pass)
                put("onvifPort", c.onvifPort); put("ptz", c.ptz)
            })
        }
        prefs.edit().putString("cameras", arr.toString()).apply()
    }
}
