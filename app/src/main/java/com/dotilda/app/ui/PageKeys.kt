package com.dotilda.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * The Kompakt volume buttons turn pages, like on an e-reader. Only paginated screens
 * register a handler; everywhere else the buttons keep changing the volume.
 */
object PageKeys {
    /** Receives -1 (volume up, previous page) or +1 (volume down, next page). */
    var handler: ((Int) -> Unit)? = null
}

@Composable
fun PageKeysEffect(onPage: (Int) -> Unit) {
    val current by rememberUpdatedState(onPage)
    DisposableEffect(Unit) {
        val handler: (Int) -> Unit = { current(it) }
        PageKeys.handler = handler
        onDispose { if (PageKeys.handler === handler) PageKeys.handler = null }
    }
}
