package com.dotilda.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.dotilda.app.YearBookExport
import com.dotilda.app.data.DotSize
import com.dotilda.app.data.Journal
import com.dotilda.app.data.SkyLayout
import com.dotilda.app.data.SkyStyle
import java.time.YearMonth
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: Prefs,
    latest: Updater.Release?,
    checking: Boolean,
    onCheck: () -> Unit,
    onOpenUpdate: () -> Unit,
    backup: Backup,
    journal: Journal,
    onAbout: () -> Unit,
    onDiagnostics: () -> Unit,
    onLicenses: () -> Unit,
    onName: () -> Unit,
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
    val bookReady = stringResource(R.string.book_exported)
    val bookTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/epub+zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupStatus = runCatching {
                context.contentResolver.openOutputStream(uri)!!.use { YearBookExport.export(context, journal, LocalDate.now().year, it) }
            }.fold({ bookReady }, { failed.format(it.message) })
        }
    }
    val sky by prefs.sky.collectAsState(initial = SkyStyle())
    var page by rememberSaveable { mutableIntStateOf(0) }
    val turn: (Int) -> Unit = { page = (page + it).coerceIn(0, PAGES - 1) }
    PageKeysEffect(turn)

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.settings), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            TextMMD("${page + 1} / $PAGES", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f)) {
            if (page == 0) {
                val name = LocalUserName.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextMMD(stringResource(R.string.settings_name), modifier = Modifier.weight(1f))
                    TextLink(name ?: stringResource(R.string.settings_name_none), onName)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextMMD(stringResource(R.string.daily_reminder), modifier = Modifier.weight(1f))
                    SwitchMMD(checked = s.reminderOn, onCheckedChange = { update(it, s.reminderMinutes) })
                }
                if (s.reminderOn) {
                    Spacer(Modifier.height(8.dp))
                    ReminderTime(s.reminderMinutes) { update(true, it) }
                }
            } else if (page == 1) {
                TextMMD(stringResource(R.string.settings_journal), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextLink(stringResource(R.string.backup_export), { pick { exportTo.launch("dotilda-${LocalDate.now()}.zip") } })
                    TextLink(stringResource(R.string.backup_import), { pick { importFrom.launch(arrayOf("application/zip", "application/octet-stream")) } })
                }
                TextLink(stringResource(R.string.book_export), { pick { bookTo.launch("dotilda-${LocalDate.now().year}.epub") } })
                backupStatus?.let { TextMMD(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(12.dp))
                HorizontalDividerMMD(thickness = 1.dp)
                Spacer(Modifier.height(12.dp))
                TextMMD(stringResource(R.string.sky_title), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Choice(stringResource(R.string.sky_dots), stringArrayResource(R.array.sky_dot_sizes).toList(), sky.dots.ordinal) {
                    scope.launch { prefs.setDotSize(DotSize.entries[it]) }
                }
                Choice(stringResource(R.string.sky_layout), stringArrayResource(R.array.sky_layouts).toList(), sky.layout.ordinal) {
                    scope.launch { prefs.setSkyLayout(SkyLayout.entries[it]) }
                }
                Spacer(Modifier.height(12.dp))
                // A preview of the month's line in the chosen style.
                val today = remember { LocalDate.now() }
                val month = remember { YearMonth.from(today) }
                val entries by remember { journal.entriesOf(month) }.collectAsState(initial = emptyList())
                CompositionLocalProvider(LocalSkyStyle provides sky) { MonthLine(month, entries, today) }
            } else {
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
                Spacer(Modifier.height(20.dp))
                HorizontalDividerMMD(thickness = 1.dp)
                // Wraps onto two lines on a narrow screen or with large text, rather than overlap.
                FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    TextLink(stringResource(R.string.about), onAbout)
                    TextLink(stringResource(R.string.licenses), onLicenses)
                    TextLink(stringResource(R.string.diagnostics), onDiagnostics)
                    // Each introduction then shows again (this page's own first, as a sign it worked).
                    TextLink(stringResource(R.string.intros_again), { scope.launch { prefs.resetIntros() } })
                }
            }
        }
        PagerRow(canPrevious = page > 0, canNext = page < PAGES - 1, onPage = turn, onBack = onBack)
    }
}

/** Name and reminder; journal and sky; updates and the rest. */
private const val PAGES = 3

/** A label and a few options in a row; the chosen one bold and underlined. */
@Composable
private fun Choice(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextMMD(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium)
        options.forEachIndexed { i, option ->
            TextMMD(
                option,
                modifier = Modifier.clickable { onSelect(i) }.padding(end = 14.dp, top = 8.dp, bottom = 8.dp),
                fontWeight = if (i == selected) FontWeight.Bold else null,
                textDecoration = if (i == selected) TextDecoration.Underline else null,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** The reminder time, moved by the hour or the quarter hour with big buttons. */
@Composable
fun ReminderTime(minutes: Int, onChange: (Int) -> Unit) {
    Column {
        TextMMD("%02d:%02d".format(minutes / 60, minutes % 60), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        // Four buttons in a row, or two rows of two when a button would be too narrow for its label.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val steps = listOf("−1 h" to -60, "+1 h" to 60, "−15 min" to -15, "+15 min" to 15)
            val rows = if (maxWidth / 4 < 76.dp * LocalDensity.current.fontScale) steps.chunked(2) else listOf(steps)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEachIndexed { i, (label, delta) ->
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
        }
    }
}
