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
import com.dotilda.app.ui.GalaxyScreen
import com.dotilda.app.ui.NotebookScreen
import com.dotilda.app.ui.PageKeys
import com.dotilda.app.ui.SettingsScreen
import com.dotilda.app.ui.TodayScreen
import com.dotilda.app.ui.UpdateScreen
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
                    // The Notebook opens at a given day when reached from the Galaxy, and returns there.
                    var notebookDate by rememberSaveable { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()
                    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                    // Flat navigation: every screen goes back to Today (Diagnostics and About to Settings).
                    val parent = when (screen) {
                        Screen.Welcome, Screen.Today -> null
                        Screen.About, Screen.Diagnostics, Screen.Update -> Screen.Settings
                        Screen.Notebook -> if (notebookDate != null) Screen.Galaxy else Screen.Today
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
                            onNotebook = {
                                notebookDate = null
                                screen = Screen.Notebook
                            },
                            onGalaxy = { screen = Screen.Galaxy },
                            onSettings = { screen = Screen.Settings },
                            newVersion = app.latestRelease?.takeIf { it.isNewer }?.version,
                            onUpdate = { screen = Screen.Update },
                        )
                        Screen.Notebook -> NotebookScreen(app.journal, notebookDate, onBack = back)
                        Screen.Galaxy -> GalaxyScreen(
                            app.journal,
                            onOpenDay = {
                                notebookDate = it.toString()
                                screen = Screen.Notebook
                            },
                            onBack = back,
                        )
                        Screen.Answer -> AnswerScreen(app.journal, onSaved = { screen = Screen.Done }, onBack = back)
                        Screen.Done -> DoneScreen(app.journal, onBack = back)
                        Screen.Update -> app.latestRelease?.let { UpdateScreen(it, onBack = back) }
                        Screen.Settings -> SettingsScreen(
                            app.prefs,
                            latest = app.latestRelease,
                            checking = app.checkingUpdate,
                            onCheck = app::checkForUpdate,
                            onOpenUpdate = { screen = Screen.Update },
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

    // Records every hardware key for Diagnostics; on paginated screens the volume buttons turn pages.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            lastKey.value = "${KeyEvent.keyCodeToString(event.keyCode)} · code ${event.keyCode} · scan ${event.scanCode}"
        }
        val pager = PageKeys.handler
        val step = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> -1
            KeyEvent.KEYCODE_VOLUME_DOWN -> 1
            else -> 0
        }
        if (pager != null && step != 0) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) pager(step)
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

private enum class Screen { Welcome, About, Today, Answer, Done, Notebook, Galaxy, Settings, Update, Diagnostics }
