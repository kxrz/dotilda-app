package com.dotilda.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/** One part of the app and Dotilda's few words about it, shown the first time it opens. */
class Intro(val key: String, val title: Int, val text: Int)

/**
 * Dotilda explains a part of the app, once: a card over the screen, on plain white (no grey veil
 * on e-ink), closed with "Got it" or Back. Settings can show them all again.
 */
@Composable
fun IntroOverlay(intro: Intro, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            // Taps outside the card don't reach the screen underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayfulDotilda(size = 44.dp, pose = Pose.Pleased)
                Spacer(Modifier.width(14.dp))
                TextMMD(stringResource(intro.title), style = MaterialTheme.typography.headlineLarge)
            }
            Spacer(Modifier.height(16.dp))
            TextMMD(stringResource(intro.text), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            ButtonMMD(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                TextMMD(stringResource(R.string.intro_ok))
            }
        }
    }
}
