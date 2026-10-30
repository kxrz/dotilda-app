package com.dotilda.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.dotilda.app.data.Answer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

/** Dotilda's signature above every question. */
@Composable
fun DotildaAsks(modifier: Modifier = Modifier, pose: Pose = Pose.Peering) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        DotildaMark(size = 32.dp, pose = pose)
        Spacer(Modifier.width(12.dp))
        TextMMD(stringResource(R.string.dotilda_asks), style = MaterialTheme.typography.titleMedium)
    }
}

/** A short aside from Dotilda, with her face; tappable when [onClick] is given. */
@Composable
fun DotildaSays(text: String, pose: Pose, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 6.dp),
    ) {
        DotildaMark(size = 22.dp, pose = pose)
        Spacer(Modifier.width(10.dp))
        TextMMD(
            text,
            textDecoration = if (onClick != null) TextDecoration.Underline else null,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** A quiet underlined text link, for secondary destinations. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    TextMMD(
        text,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        textDecoration = TextDecoration.Underline,
        style = style,
    )
}

/** Back on the left, the main action on the right. */
@Composable
fun ActionRow(
    back: String,
    onBack: () -> Unit,
    action: String? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth()) {
        OutlinedButtonMMD(onClick = onBack, modifier = Modifier.weight(1f)) { TextMMD(back) }
        if (action != null) {
            Spacer(Modifier.width(12.dp))
            ButtonMMD(onClick = onAction, enabled = actionEnabled, modifier = Modifier.weight(1f)) { TextMMD(action) }
        }
    }
}

@Composable
fun weatherLabel(level: Int): String = stringArrayResource(R.array.weather_labels)[level]

@Composable
fun Answer.summary(): String = when (this) {
    is Answer.Text -> "“$text”"
    is Answer.Word -> word
    is Answer.Weather -> weatherLabel(level)
    is Answer.Choice -> label
    is Answer.Things -> items.joinToString(" · ")
    is Answer.Photo -> caption.ifEmpty { stringResource(R.string.photo_summary) }
    is Answer.Letter -> stringResource(R.string.letter_summary)
}

/** Previous page, Back, next page: the three buttons under every paginated screen. */
@Composable
fun PagerRow(
    canPrevious: Boolean,
    canNext: Boolean,
    onPage: (Int) -> Unit,
    onBack: () -> Unit,
    previous: String = "‹",
    next: String = "›",
) {
    Row(Modifier.fillMaxWidth()) {
        OutlinedButtonMMD(onClick = { onPage(-1) }, enabled = canPrevious, modifier = Modifier.weight(1f)) {
            TextMMD(previous, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButtonMMD(onClick = onBack, modifier = Modifier.weight(1.6f)) {
            TextMMD(stringResource(R.string.back))
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButtonMMD(onClick = { onPage(1) }, enabled = canNext, modifier = Modifier.weight(1f)) {
            TextMMD(next, maxLines = 1)
        }
    }
}
