package com.dotilda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dotilda.app.R
import com.mudita.mmd.components.text.TextMMD

/** Characters per page at the Kompakt's size, in small type: no scrolling, pages instead. */
private const val PAGE_CHARS = 700

/** The open source notices and licences (Apache 2.0, SIL OFL 1.1 for Lato), page by page. */
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pages = remember {
        val text = listOf("NOTICE.txt", "OFL-1.1.txt", "APACHE-2.0.txt").joinToString("\n\n") { name ->
            context.assets.open("licenses/$name").bufferedReader().use { it.readText() }.trim()
        }
        paginate(text, PAGE_CHARS)
    }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val turn: (Int) -> Unit = { page = (page + it).coerceIn(0, pages.lastIndex) }
    PageKeysEffect(turn)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextMMD(stringResource(R.string.licenses), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
            TextMMD("${page + 1} / ${pages.size}", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(16.dp))
        TextMMD(pages[page], modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        PagerRow(canPrevious = page > 0, canNext = page < pages.lastIndex, onPage = turn, onBack = onBack)
    }
}

/** Cuts [text] into pages of about [size] characters, at paragraph breaks, else at word breaks. */
fun paginate(text: String, size: Int): List<String> {
    val pages = mutableListOf<String>()
    val page = StringBuilder()
    fun flush() {
        if (page.isNotBlank()) pages += page.toString().trim()
        page.clear()
    }
    for (paragraph in text.split(Regex("\\n\\s*\\n"))) {
        var rest = paragraph.trim()
        while (rest.isNotEmpty()) {
            val room = size - page.length
            if (rest.length + 2 <= room) {
                if (page.isNotEmpty()) page.append("\n\n")
                page.append(rest)
                rest = ""
            } else if (page.isNotEmpty() && room < size / 3) {
                flush()
            } else {
                val cut = rest.lastIndexOf(' ', (room - 2).coerceAtLeast(1)).takeIf { it > 0 } ?: minOf(rest.length, room.coerceAtLeast(1))
                if (page.isNotEmpty()) page.append("\n\n")
                page.append(rest.substring(0, cut).trim())
                rest = rest.substring(cut).trim()
                flush()
            }
        }
    }
    flush()
    return pages
}
