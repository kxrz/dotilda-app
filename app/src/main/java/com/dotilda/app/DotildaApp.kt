package com.dotilda.app

import android.app.Application
import com.dotilda.app.data.DotildaDatabase
import com.dotilda.app.data.syncQuestionBank
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async

class DotildaApp : Application() {
    lateinit var db: DotildaDatabase
        private set

    /** Completes once assets/questions.json has been copied into Room. */
    lateinit var bankReady: Deferred<Unit>
        private set

    override fun onCreate() {
        super.onCreate()
        db = DotildaDatabase.create(this)
        bankReady = MainScope().async { syncQuestionBank(this@DotildaApp, db.questionDao()) }
    }
}
