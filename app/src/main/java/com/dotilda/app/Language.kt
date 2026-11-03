package com.dotilda.app

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/**
 * The language chosen in Settings, or the phone's ("Automatic"). Kept in plain
 * SharedPreferences because it is needed synchronously, before the first screen is drawn.
 * Works on Android 11 and 12 too, which have no per-app language setting.
 */
object Language {
    /** Languages Dotilda speaks, each with its name in itself. */
    val available = listOf("en" to "English", "fr" to "Français", "pl" to "Polski")

    private const val FILE = "language"
    private const val KEY = "tag"

    /** The phone's own language, kept before any override replaces the default. */
    private val system: Locale = Resources.getSystem().configuration.locales[0]

    /** The chosen language tag, or null to follow the phone. */
    fun chosen(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)

    fun choose(context: Context, tag: String?) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().apply {
            if (tag == null) remove(KEY) else putString(KEY, tag)
        }.commit()
    }

    /** [base] speaking the chosen language (unchanged when following the phone). */
    fun wrap(base: Context): Context {
        val tag = chosen(base)
        val locale = tag?.let(Locale::forLanguageTag) ?: system
        Locale.setDefault(locale)
        if (tag == null) return base
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(config)
    }
}
