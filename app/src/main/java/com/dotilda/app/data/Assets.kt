package com.dotilda.app.data

import android.content.Context
import com.dotilda.app.R

/**
 * Reads assets/<name>.json, or assets/<name>-<lang>.json when the interface speaks a language
 * that has its own (questions-pl.json, journeys-pl.json). The language is the interface's
 * (`app_locale`), so questions never differ from the screens around them.
 */
internal fun readLocalizedAsset(context: Context, name: String): String {
    val localized = "$name-${context.getString(R.string.app_locale)}.json"
    val file = if (localized in context.assets.list("").orEmpty()) localized else "$name.json"
    return context.assets.open(file).bufferedReader().use { it.readText() }
}
