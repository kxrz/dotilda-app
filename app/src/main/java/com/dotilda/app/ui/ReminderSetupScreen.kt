package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.Reminder
import com.dotilda.app.data.Prefs
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch

/** Onboarding, step 2: when Dotilda should come by. Then straight to the first question. */
@Composable
fun ReminderSetupScreen(prefs: Prefs, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var minutes by rememberSaveable { mutableIntStateOf(Prefs.DEFAULT_REMINDER_MINUTES) }

    fun finish(on: Boolean) {
        scope.launch {
            prefs.setReminder(on, minutes)
            Reminder.schedule(context, on, minutes)
            onDone()
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        DotildaQuestion(stringResource(R.string.setup_question), style = MaterialTheme.typography.headlineLarge, pose = Pose.Curious)
        Spacer(Modifier.height(12.dp))
        TextMMD(stringResource(R.string.setup_hint), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        ReminderTime(minutes) { minutes = it }
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = { finish(true) }, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.setup_remind_me))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(onClick = { finish(false) }, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.setup_no_reminder))
        }
    }
}
