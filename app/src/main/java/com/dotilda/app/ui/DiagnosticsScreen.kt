package com.dotilda.app.ui

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.DotildaApp
import com.dotilda.app.R
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD

/** Facts to collect on the test Kompakt: screen size, density, hardware key codes. */
@Composable
fun DiagnosticsScreen(app: DotildaApp, lastKey: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val metrics = context.resources.displayMetrics
    val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
    var count by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        app.bankReady.await()
        count = app.db.questionDao().count()
    }

    val rows = listOf(
        "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Build" to Build.DISPLAY,
        "Screen" to "${bounds.width()} × ${bounds.height()} px",
        "Density" to "${metrics.densityDpi} dpi (×${metrics.density})",
        "Viewport" to "${config.screenWidthDp} × ${config.screenHeightDp} dp",
        "Questions" to (count?.toString() ?: "…"),
        "Last key" to (lastKey ?: stringResource(R.string.press_a_button)),
    )

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextMMD(stringResource(R.string.diagnostics), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                TextMMD(label, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall)
                TextMMD(value, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDividerMMD(thickness = 1.dp)
        }
        Spacer(Modifier.weight(1f))
        ButtonMMD(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            TextMMD(stringResource(R.string.back))
        }
    }
}
