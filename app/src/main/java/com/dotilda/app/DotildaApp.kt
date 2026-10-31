package com.dotilda.app

import android.app.Application
import com.dotilda.app.data.Backup
import com.dotilda.app.data.DotildaDatabase
import com.dotilda.app.data.Journal
import com.dotilda.app.data.Prefs
import com.dotilda.app.data.loadJourneys
import com.dotilda.app.data.syncQuestionBank
import com.dotilda.app.update.Updater
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DotildaApp : Application() {
    lateinit var db: DotildaDatabase
        private set

    lateinit var prefs: Prefs
        private set

    lateinit var journal: Journal
        private set

    lateinit var backup: Backup
        private set

    /** Completes once assets/questions.json has been copied into Room. */
    lateinit var bankReady: Deferred<Unit>
        private set

    /** The last update check: null before it answers, or when offline. */
    var latestRelease by mutableStateOf<Updater.Release?>(null)
        private set

    var checkingUpdate by mutableStateOf(false)
        private set

    /** Set for this launch when the app has just been updated, so Dotilda can mention it. */
    var justUpdatedTo by mutableStateOf<String?>(null)
        private set

    /** Set when the app could not even start: MainActivity then shows the crash report only. */
    var startupError: Throwable? = null
        private set

    fun checkForUpdate() {
        if (checkingUpdate) return
        checkingUpdate = true
        MainScope().launch {
            latestRelease = Updater.latest()
            checkingUpdate = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        try {
            start()
        } catch (e: Exception) {
            CrashLog.save(this, e)
            startupError = e
        }
    }

    private fun start() {
        db = DotildaDatabase.create(this)
        prefs = Prefs(this)
        bankReady = MainScope().async { syncQuestionBank(this@DotildaApp, db.questionDao()) }
        journal = Journal(db, prefs, bankReady, loadJourneys(this))
        backup = Backup(db.entryDao(), prefs, filesDir)

        Reminder.createChannel(this)
        SpiraTimer.createChannel(this)
        MainScope().launch {
            val settings = prefs.settingsNow()
            Reminder.schedule(this@DotildaApp, settings.reminderOn, settings.reminderMinutes)
            val previous = prefs.swapLastVersion(BuildConfig.VERSION_NAME)
            if (previous != null && previous != BuildConfig.VERSION_NAME) {
                justUpdatedTo = BuildConfig.VERSION_NAME
                prefs.setUpdatedOn(BuildConfig.VERSION_NAME, java.time.LocalDate.now().toString())
            }
            if (settings.checkUpdates) checkForUpdate()
        }
    }
}
