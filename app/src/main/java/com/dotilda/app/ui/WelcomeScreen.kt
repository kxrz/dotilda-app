package com.dotilda.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * First launch (and "About"): who Dotilda is and how the ritual works, on one airy page.
 * Three short promises, each led by a dot from the galaxy, instead of paragraphs.
 */
@Composable
fun WelcomeScreen(button: String, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        PlayfulDotilda(size = 96.dp, pose = Pose.Pleased)
        Spacer(Modifier.height(20.dp))
        TextMMD(stringResource(R.string.dotilda), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        TextMMD(
            stringResource(R.string.welcome_tagline),
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.fillMaxWidth()) {
            Promise(DotShape.Full, R.string.welcome_1)
            Promise(DotShape.Moon(3), R.string.welcome_2)
            Promise(DotShape.Double, R.string.welcome_3)
        }
        Spacer(Modifier.weight(1.4f))
        ButtonMMD(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { TextMMD(button) }
    }
}

@Composable
private fun Promise(dot: DotShape, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DayDot(dot, size = 14.dp)
        Spacer(Modifier.width(18.dp))
        TextMMD(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}
