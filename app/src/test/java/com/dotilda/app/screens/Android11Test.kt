package com.dotilda.app.screens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dotilda.app.DotildaApp
import com.dotilda.app.Reminder
import com.dotilda.app.SpiraTimer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Android 11 (API 30), Dotilda's oldest: its framework lacks Android 12's exact-alarm question,
 * which once closed the app at every launch (Xteink S4).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class Android11Test {
    private val app get() = ApplicationProvider.getApplicationContext<DotildaApp>()

    @Test
    fun alarmsWorkOnAndroid11() {
        Reminder.schedule(app, on = true, minutes = 20 * 60)
        SpiraTimer.start(app, System.currentTimeMillis() + 60_000)
        SpiraTimer.cancel(app)
    }
}
