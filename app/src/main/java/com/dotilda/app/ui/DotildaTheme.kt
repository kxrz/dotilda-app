package com.dotilda.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dotilda.app.data.SkyStyle
import androidx.compose.ui.Modifier
import com.mudita.mmd.ThemeMMD

/** MMD's monochrome e-ink theme (no ripples) on a plain white page, with the chosen sky style. */
@Composable
fun DotildaTheme(sky: SkyStyle = SkyStyle(), content: @Composable () -> Unit) {
    ThemeMMD {
        CompositionLocalProvider(LocalSkyStyle provides sky) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Surface(modifier = Modifier.safeDrawingPadding(), content = content)
            }
        }
    }
}
