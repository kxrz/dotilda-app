package com.dotilda.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
    DotildaBeside(
        pose = pose,
        markSize = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp),
    ) {
        TextMMD(
            text,
            textDecoration = if (onClick != null) TextDecoration.Underline else null,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

/** Today's date, read again whenever the screen comes back to the foreground. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) today = LocalDate.now()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return today
}
