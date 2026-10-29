package com.dotilda.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mudita.mmd.ThemeMMD

/** MMD's monochrome e-ink theme (no ripples) on a plain white page. */
@Composable
fun DotildaTheme(content: @Composable () -> Unit) {
    ThemeMMD {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Surface(modifier = Modifier.safeDrawingPadding(), content = content)
        }
    }
}
