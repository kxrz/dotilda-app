package com.dotilda.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.BuildConfig
import com.dotilda.app.R
import com.dotilda.app.Reminder
import com.dotilda.app.data.Backup
import com.dotilda.app.data.Prefs
import java.time.LocalDate
import com.dotilda.app.update.Updater
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.switcher.SwitchMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch

const val DAY_MINUTES = 24 * 60

@Composable
fun SettingsScreen(
    prefs: Prefs,
    latest: Updater.Release?,
    checking: Boolean,
    onCheck: () -> Unit,
    onOpenUpdate: () -> Unit,
    backup: Backup,
    onAbout: () -> Unit,
    onDiagnostics: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by prefs.settings.collectAsState(initial = null)
    val s = settings ?: return
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val noPicker = stringResource(R.string.backup_no_picker)
    val exported = stringResource(R.string.backup_exported)
    val failed = stringResource(R.string.backup_failed)

    val exportTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupStatus = runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { backup.export(it, LocalDate.now().toString()) }
            }.fold({ exported }, { failed.format(it.message) })
        }
    }
    val importFrom = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupStatus = runCatching {
                context.contentResolver.openInputStream(uri)!!.use { backup.import(it) }
            }.fold({ context.resources.getQuantityString(R.plurals.backup_imported, it, it) }, { failed.format(it.message) })
        }
    }
    fun pick(launch: () -> Unit) {
        try {
            launch()
        } catch (e: ActivityNotFoundException) {
            backupStatus = noPicker
        }
    }

    fun update(on: Boolean, minutes: Int) {
        val m = Math.floorMod(minutes, DAY_MINUTES)
        scope.launch {
            prefs.setReminder(on, m)
            Reminder.schedule(context, on, m)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextMMD(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.daily_reminder), modifier = Modifier.weight(1f))
            SwitchMMD(checked = s.reminderOn, onCheckedChange = { update(it, s.reminderMinutes) })
        }
        if (s.reminderOn) {
            Spacer(Modifier.height(8.dp))
            ReminderTime(s.reminderMinutes) { update(true, it) }
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDividerMMD(thickness = 1.dp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.check_updates), modifier = Modifier.weight(1f))
            SwitchMMD(
                checked = s.checkUpdates,
                onCheckedChange = { on ->
                    scope.launch { prefs.setCheckUpdates(on) }
                    if (on) onCheck()
                },
            )
        }
        val version = BuildConfig.VERSION_NAME
        when {
            checking -> TextMMD(stringResource(R.string.checking_updates), style = MaterialTheme.typography.bodySmall)
            latest?.isNewer == true -> TextLink(stringResource(R.string.update_available, latest.version), onOpenUpdate)
            latest != null -> TextMMD(stringResource(R.string.up_to_date, version), style = MaterialTheme.typography.bodySmall)
            else -> TextLink(stringResource(R.string.check_now, version), onCheck)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDividerMMD(thickness = 1.dp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextLink(stringResource(R.string.backup_export), { pick { exportTo.launch("dotilda-${LocalDate.now()}.zip") } })
            TextLink(stringResource(R.string.backup_import), { pick { importFrom.launch(arrayOf("application/zip", "application/octet-stream")) } })
        }
        backupStatus?.let { TextMMD(it, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextLink(stringResource(R.string.about), onAbout)
            TextLink(stringResource(R.string.diagnostics), onDiagnostics)
        }
        Spacer(Modifier.weight(1f))
        ActionRow(back = stringResource(R.string.back), onBack = onBack)
    }
}

/** The reminder time, moved by the hour or the quarter hour with big buttons. */
@Composable
fun ReminderTime(minutes: Int, onChange: (Int) -> Unit) {
    Column {
        TextMMD("%02d:%02d".format(minutes / 60, minutes % 60), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("−1 h" to -60, "+1 h" to 60, "−15 min" to -15, "+15 min" to 15).forEachIndexed { i, (label, delta) ->
                if (i > 0) Spacer(Modifier.width(6.dp))
                OutlinedButtonMMD(
                    onClick = { onChange(Math.floorMod(minutes + delta, DAY_MINUTES)) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) { TextMMD(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
            }
        }
    }
}
