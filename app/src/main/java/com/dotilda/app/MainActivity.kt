package com.dotilda.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.dotilda.app.ui.AnswerScreen
import com.dotilda.app.ui.DiagnosticsScreen
import com.dotilda.app.ui.DoneScreen
import com.dotilda.app.ui.DotildaTheme
import com.dotilda.app.ui.SettingsScreen
import com.dotilda.app.ui.TodayScreen
import com.dotilda.app.ui.WelcomeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val lastKey = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DotildaApp
        setContent {
            DotildaTheme {
                val settings by app.prefs.settings.collectAsState(initial = null)
                val welcomed = settings?.welcomed
                if (welcomed != null) {
                    var screen by rememberSaveable { mutableStateOf(if (welcomed) Screen.Today else Screen.Welcome) }
                    val scope = rememberCoroutineScope()
                    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                    // Flat navigation: every screen goes back to Today (Diagnostics and About to Settings).
                    val parent = when (screen) {
                        Screen.Welcome, Screen.Today -> null
                        Screen.About, Screen.Diagnostics -> Screen.Settings
                        else -> Screen.Today
                    }
                    BackHandler(enabled = parent != null) { parent?.let { screen = it } }
                    val back = { screen = parent ?: Screen.Today }

                    when (screen) {
                        Screen.Welcome -> WelcomeScreen(stringResource(R.string.begin)) {
                            if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            scope.launch { app.prefs.setWelcomed() }
                            screen = Screen.Today
                        }
                        Screen.About -> WelcomeScreen(stringResource(R.string.back), back)
                        Screen.Today -> TodayScreen(
                            app.journal,
                            onAnswer = { screen = Screen.Answer },
                            onSettings = { screen = Screen.Settings },
                        )
                        Screen.Answer -> AnswerScreen(app.journal, onSaved = { screen = Screen.Done }, onBack = back)
                        Screen.Done -> DoneScreen(app.journal, onBack = back)
                        Screen.Settings -> SettingsScreen(
                            app.prefs,
                            onAbout = { screen = Screen.About },
                            onDiagnostics = { screen = Screen.Diagnostics },
                            onBack = back,
                        )
                        Screen.Diagnostics -> DiagnosticsScreen(app, lastKey.value, onBack = back)
                    }
                }
            }
        }
    }

    // Records every hardware key so the Diagnostics screen can show it.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            lastKey.value = "${KeyEvent.keyCodeToString(event.keyCode)} · code ${event.keyCode} · scan ${event.scanCode}"
        }
        return super.dispatchKeyEvent(event)
    }
}

private enum class Screen { Welcome, About, Today, Answer, Done, Settings, Diagnostics }
