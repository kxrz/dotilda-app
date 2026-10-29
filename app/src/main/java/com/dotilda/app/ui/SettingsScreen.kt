package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.Reminder
import com.dotilda.app.data.Prefs
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch

private const val DAY_MINUTES = 24 * 60

@Composable
fun SettingsScreen(prefs: Prefs, onAbout: () -> Unit, onDiagnostics: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by prefs.settings.collectAsState(initial = null)
    val s = settings ?: return

    fun update(on: Boolean, minutes: Int) {
        val m = Math.floorMod(minutes, DAY_MINUTES)
        scope.launch {
            prefs.setReminder(on, m)
            Reminder.schedule(context, on, m)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextMMD(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.daily_reminder), modifier = Modifier.weight(1f))
            SwitchMMD(checked = s.reminderOn, onCheckedChange = { update(it, s.reminderMinutes) })
        }
        if (s.reminderOn) {
            Spacer(Modifier.height(12.dp))
            TextMMD(
                "%02d:%02d".format(s.reminderMinutes / 60, s.reminderMinutes % 60),
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("−1 h" to -60, "+1 h" to 60, "−15 min" to -15, "+15 min" to 15).forEachIndexed { i, (label, delta) ->
                    if (i > 0) Spacer(Modifier.width(6.dp))
                    OutlinedButtonMMD(
                        onClick = { update(true, s.reminderMinutes + delta) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) { TextMMD(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        HorizontalDividerMMD(thickness = 1.dp)
        TextLink(stringResource(R.string.about), onAbout, Modifier.padding(top = 8.dp))
        TextLink(stringResource(R.string.diagnostics), onDiagnostics)
        Spacer(Modifier.weight(1f))
        ActionRow(back = stringResource(R.string.back), onBack = onBack)
    }
}
