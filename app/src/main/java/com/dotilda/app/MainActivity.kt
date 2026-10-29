package com.dotilda.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dotilda.app.ui.DiagnosticsScreen
import com.dotilda.app.ui.DotildaTheme
import com.dotilda.app.ui.TodayScreen

class MainActivity : ComponentActivity() {
    private val lastKey = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DotildaApp
        setContent {
            DotildaTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.Today) }
                when (screen) {
                    Screen.Today -> TodayScreen(app, onOpenDiagnostics = { screen = Screen.Diagnostics })
                    Screen.Diagnostics -> {
                        BackHandler { screen = Screen.Today }
                        DiagnosticsScreen(app, lastKey.value, onBack = { screen = Screen.Today })
                    }
                }
            }
        }
    }

    // Records every hardware key so the Kompakt page buttons can be identified on device.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            lastKey.value = "${KeyEvent.keyCodeToString(event.keyCode)} · code ${event.keyCode} · scan ${event.scanCode}"
        }
        return super.dispatchKeyEvent(event)
    }
}

private enum class Screen { Today, Diagnostics }
