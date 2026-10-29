package com.dotilda.app

import android.app.Application
import com.dotilda.app.data.DotildaDatabase
import com.dotilda.app.data.Journal
import com.dotilda.app.data.Prefs
import com.dotilda.app.data.syncQuestionBank
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

    /** Completes once assets/questions.json has been copied into Room. */
    lateinit var bankReady: Deferred<Unit>
        private set

    override fun onCreate() {
        super.onCreate()
        db = DotildaDatabase.create(this)
        prefs = Prefs(this)
        bankReady = MainScope().async { syncQuestionBank(this@DotildaApp, db.questionDao()) }
        journal = Journal(db, prefs, bankReady)

        Reminder.createChannel(this)
        MainScope().launch {
            val settings = prefs.settingsNow()
            Reminder.schedule(this@DotildaApp, settings.reminderOn, settings.reminderMinutes)
        }
    }
}
