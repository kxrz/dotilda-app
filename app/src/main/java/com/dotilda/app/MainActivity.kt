package com.dotilda.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.ActivityNotFoundException
import android.widget.Toast
import kotlinx.coroutines.MainScope
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
import com.dotilda.app.data.SkyStyle
import com.dotilda.app.ui.DotildaTheme
import com.dotilda.app.ui.GalaxyScreen
import com.dotilda.app.ui.Intro
import com.dotilda.app.ui.IntroOverlay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.dotilda.app.ui.JourneysScreen
import com.dotilda.app.ui.LicensesScreen
import com.dotilda.app.ui.LocalUserName
import com.dotilda.app.ui.NameScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.dotilda.app.ui.ReviewScreen
import com.dotilda.app.ui.NotebookScreen
import com.dotilda.app.ui.PageKeys
import com.dotilda.app.ui.ReminderSetupScreen
import com.dotilda.app.ui.SettingsScreen
import com.dotilda.app.ui.SpiraMode
import com.dotilda.app.ui.SpiraScreen
import com.dotilda.app.ui.TodayScreen
import com.dotilda.app.ui.UpdateScreen
import com.dotilda.app.ui.WelcomeScreen
import java.time.YearMonth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val lastKey = mutableStateOf<String?>(null)
    private var crashReport: String? = null
    private val saveCrash = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val text = crashReport
        if (uri != null && text != null) {
            runCatching { contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DotildaApp
        // After a crash (or if Dotilda cannot start at all), the report comes first, readable.
        val failed = app.startupError
        val report = if (failed != null) CrashLog.read(this) else CrashLog.unseen(this)
        if (report != null) {
            crashReport = report
            showCrashReport(
                report,
                onSave = {
                    try {
                        saveCrash.launch("dotilda-crash.txt")
                    } catch (e: ActivityNotFoundException) {
                        // No file picker (Xteink S4): straight into Download/Dotilda.
                        MainScope().launch {
                            val where = runCatching {
                                getString(R.string.saved_in, DownloadsFolder.save(this@MainActivity, "dotilda-crash.txt", "text/plain") { it.write(report.toByteArray()) })
                            }.getOrElse { it.message.orEmpty() }
                            Toast.makeText(this@MainActivity, where, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onContinue = if (failed != null) null else ({
                    CrashLog.markSeen(this)
                    recreate()
                }),
            )
            return
        }
        setContent {
            val sky by app.prefs.sky.collectAsState(initial = SkyStyle())
            val name by app.prefs.name.collectAsState(initial = "")
            DotildaTheme(sky) { CompositionLocalProvider(LocalUserName provides name.ifBlank { null }) {
                val settings by app.prefs.settings.collectAsState(initial = null)
                val welcomed = settings?.welcomed
                if (welcomed != null) {
                    // Those who had Dotilda before she asked for names are asked once, at their next launch.
                    val askName = welcomed && settings?.nameAsked == false
                    var screen by rememberSaveable {
                        mutableStateOf(
                            when {
                                !welcomed -> Screen.Welcome
                                askName -> Screen.Name
                                else -> Screen.Today
                            },
                        )
                    }
                    // The Notebook opens at a given day (from the Galaxy, a review or a letter) and returns there.
                    var notebookDate by rememberSaveable { mutableStateOf<String?>(null) }
                    var notebookFrom by rememberSaveable { mutableStateOf(Screen.Today) }
                    var reviewMonth by rememberSaveable { mutableStateOf<String?>(null) }
                    var reviewMonths by rememberSaveable { mutableStateOf(1) }
                    var reviewFrom by rememberSaveable { mutableStateOf(Screen.Today) }
                    fun openDay(date: String, from: Screen) {
                        notebookDate = date
                        notebookFrom = from
                        screen = Screen.Notebook
                    }
                    fun openReview(month: YearMonth, from: Screen, months: Int = 1) {
                        reviewMonth = month.toString()
                        reviewMonths = months
                        reviewFrom = from
                        screen = Screen.Review
                    }
                    // spira opened from Answer ("breathe first") returns to the question.
                    var spiraFromAnswer by rememberSaveable { mutableStateOf(false) }
                    var nameFromSettings by rememberSaveable { mutableStateOf(false) }
                    // Where the name question leads: the reminder during onboarding, Today after an update.
                    var nameThen by rememberSaveable { mutableStateOf(if (askName) Screen.Today else Screen.Setup) }
                    val scope = rememberCoroutineScope()
                    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                    // Flat navigation: every screen goes back to Today (Diagnostics and About to Settings).
                    val parent = when (screen) {
                        Screen.Welcome, Screen.Today, Screen.Setup -> null
                        Screen.Name -> if (nameFromSettings) Screen.Settings else null
                        Screen.Spira -> if (spiraFromAnswer) Screen.Answer else Screen.Today
                        Screen.About, Screen.Diagnostics, Screen.Update, Screen.Licenses -> Screen.Settings
                        Screen.Notebook -> notebookFrom
                        Screen.Review -> reviewFrom
                        else -> Screen.Today
                    }
                    BackHandler(enabled = parent != null) { parent?.let { screen = it } }
                    val back = { screen = parent ?: Screen.Today }
                    // null until read, so an introduction never flashes on a screen whose intro was seen.
                    val introsSeen by app.prefs.introsSeen.collectAsState(initial = null)

                    Box(Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.Welcome -> WelcomeScreen(stringResource(R.string.begin)) {
                            if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            scope.launch { app.prefs.setWelcomed() }
                            screen = Screen.Name
                        }
                        // Onboarding asks for a name, then the reminder time; Settings can change the name.
                        Screen.Name -> NameScreen(app.prefs) { screen = if (nameFromSettings) Screen.Settings else nameThen }
                        // Onboarding ends on the first question, straight away.
                        Screen.Setup -> ReminderSetupScreen(app.prefs) { screen = Screen.Answer }
                        Screen.Spira -> SpiraScreen(
                            app.prefs,
                            start = SpiraMode.Breathe,
                            onWrite = if (spiraFromAnswer) back else null,
                            onBack = back,
                        )
                        Screen.About -> WelcomeScreen(stringResource(R.string.back), back)
                        Screen.Today -> TodayScreen(
                            app.journal,
                            onAnswer = { screen = Screen.Answer },
                            onNotebook = {
                                notebookDate = null
                                notebookFrom = Screen.Today
                                screen = Screen.Notebook
                            },
                            onJourneys = { screen = Screen.Journeys },
                            onLetter = { openDay(it, Screen.Today) },
                            onReview = { month, months -> openReview(month, Screen.Today, months) },
                            onGalaxy = { screen = Screen.Galaxy },
                            onSpira = {
                                spiraFromAnswer = false
                                screen = Screen.Spira
                            },
                            onSettings = { screen = Screen.Settings },
                            newVersion = app.latestRelease?.takeIf { it.isNewer }?.version,
                            justUpdatedTo = app.justUpdatedTo,
                            onUpdate = { screen = Screen.Update },
                        )
                        Screen.Notebook -> NotebookScreen(app.journal, notebookDate, onBack = back)
                        Screen.Galaxy -> GalaxyScreen(
                            app.journal,
                            onOpenDay = { openDay(it.toString(), Screen.Galaxy) },
                            onOpenMonth = { openReview(it, Screen.Galaxy) },
                            onOpenSeason = { openReview(it, Screen.Galaxy, months = 3) },
                            onBack = back,
                        )
                        Screen.Journeys -> JourneysScreen(app.journal, onStarted = { screen = Screen.Today }, onBack = back)
                        Screen.Review -> reviewMonth?.let { m ->
                            ReviewScreen(app.journal, YearMonth.parse(m), reviewMonths, onOpenDay = { openDay(it.toString(), Screen.Review) }, onBack = back)
                        }
                        Screen.Answer -> AnswerScreen(
                            app.journal,
                            onSaved = { screen = Screen.Done },
                            onBreathe = {
                                spiraFromAnswer = true
                                screen = Screen.Spira
                            },
                            onBack = back,
                        )
                        Screen.Done -> DoneScreen(app.journal, onBack = back)
                        Screen.Update -> app.latestRelease?.let { UpdateScreen(it, onBack = back) }
                        Screen.Settings -> SettingsScreen(
                            app.prefs,
                            latest = app.latestRelease,
                            checking = app.checkingUpdate,
                            onCheck = app::checkForUpdate,
                            onOpenUpdate = { screen = Screen.Update },
                            backup = app.backup,
                            journal = app.journal,
                            onAbout = { screen = Screen.About },
                            onDiagnostics = { screen = Screen.Diagnostics },
                            onLicenses = { screen = Screen.Licenses },
                            onName = {
                                nameFromSettings = true
                                screen = Screen.Name
                            },
                            onBack = back,
                        )
                        Screen.Diagnostics -> DiagnosticsScreen(app, lastKey.value, onBack = back)
                        Screen.Licenses -> LicensesScreen(onBack = back)
                    }
                    // Dotilda introduces each part the first time it opens, over the screen itself.
                    val intro = introFor(screen)
                    if (intro != null && introsSeen?.contains(intro.key) == false) {
                        IntroOverlay(intro) { scope.launch { app.prefs.markIntroSeen(intro.key) } }
                    }
                    }
                }
            } }
        }
    }

    private fun introFor(screen: Screen): Intro? = when (screen) {
        Screen.Today -> Intro("today", R.string.intro_today_title, R.string.intro_today)
        Screen.Notebook -> Intro("notebook", R.string.intro_notebook_title, R.string.intro_notebook)
        Screen.Galaxy -> Intro("galaxy", R.string.intro_galaxy_title, R.string.intro_galaxy)
        Screen.Journeys -> Intro("journeys", R.string.intro_journeys_title, R.string.intro_journeys)
        Screen.Spira -> Intro("spira", R.string.intro_spira_title, R.string.intro_spira)
        Screen.Review -> Intro("review", R.string.intro_review_title, R.string.intro_review)
        Screen.Settings -> Intro("settings", R.string.intro_settings_title, R.string.intro_settings)
        else -> null
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

private enum class Screen { Welcome, Name, Setup, Spira, About, Today, Answer, Done, Notebook, Galaxy, Journeys, Review, Settings, Update, Diagnostics, Licenses }
