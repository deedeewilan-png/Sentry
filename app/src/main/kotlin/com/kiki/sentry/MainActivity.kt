package com.kiki.sentry

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

sealed interface Screen {
    data object Home : Screen
    data class Viewer(val id: String) : Screen
    data class Edit(val id: String?) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent { SentryTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val store = remember { CameraStore(ctx) }
    val cameras = remember { mutableStateListOf<Camera>().also { it.addAll(store.load()) } }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    fun persist() = store.save(cameras.toList())

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            cameras = cameras,
            onOpen = { screen = Screen.Viewer(it.id) },
            onAdd = { screen = Screen.Edit(null) },
            onEdit = { screen = Screen.Edit(it.id) }
        )
        is Screen.Viewer -> {
            val cam = cameras.firstOrNull { it.id == s.id }
            if (cam == null) screen = Screen.Home
            else ViewerScreen(cam, onBack = { screen = Screen.Home }, onEdit = { screen = Screen.Edit(cam.id) })
        }
        is Screen.Edit -> {
            val initial = cameras.firstOrNull { it.id == s.id }
            EditScreen(
                initial = initial,
                onBack = { screen = Screen.Home },
                onSave = { cam ->
                    val i = cameras.indexOfFirst { it.id == cam.id }
                    if (i >= 0) cameras[i] = cam else cameras.add(cam)
                    persist()
                    screen = Screen.Viewer(cam.id)
                },
                onDelete = initial?.let { c ->
                    {
                        cameras.removeAll { it.id == c.id }
                        persist()
                        screen = Screen.Home
                    }
                }
            )
        }
    }
}
