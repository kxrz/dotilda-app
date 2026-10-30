package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dotilda.app.BuildConfig
import com.dotilda.app.R
import com.dotilda.app.update.Updater
import com.dotilda.app.update.plainNotes
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.launch

/** What's new in the next version, and the button that installs it. */
@Composable
fun UpdateScreen(release: Updater.Release, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val downloading = stringResource(R.string.downloading)
    val allowInstalls = stringResource(R.string.allow_installs)
    val failed = stringResource(R.string.update_failed)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DotildaMark(size = 40.dp, pose = if (busy) Pose.Peering else Pose.Pleased)
            Spacer(Modifier.width(12.dp))
            TextMMD(stringResource(R.string.update_title, release.version), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(8.dp))
        TextMMD(stringResource(R.string.update_intro), fontStyle = FontStyle.Italic)
        Spacer(Modifier.height(16.dp))
        TextMMD(
            release.notes?.let(::plainNotes) ?: stringResource(R.string.update_no_notes),
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        val note = when {
            status != null -> status
            BuildConfig.DEBUG -> stringResource(R.string.update_debug)
            else -> null
        }
        note?.let {
            TextMMD(it, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
        }
        ActionRow(
            back = stringResource(R.string.back),
            onBack = onBack,
            action = stringResource(R.string.update_now),
            actionEnabled = release.canInstall && !busy,
            onAction = {
                if (!Updater.ensureInstallAllowed(context)) {
                    status = allowInstalls
                } else {
                    busy = true
                    status = downloading
                    scope.launch {
                        val result = runCatching { Updater.install(context, Updater.download(context, release)) }
                        busy = false
                        status = result.exceptionOrNull()?.let { failed.format(it.message) }
                    }
                }
            },
        )
    }
}
