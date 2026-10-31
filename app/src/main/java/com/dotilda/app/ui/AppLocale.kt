package com.dotilda.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dotilda.app.R
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The language the interface is shown in (`app_locale` in strings.xml), not the phone's:
 * on a phone in a language Dotilda doesn't speak yet, dates stay in English with the rest.
 */
@Composable
fun appLocale(): Locale = Locale.forLanguageTag(stringResource(R.string.app_locale))

/** A month on its own, as a title ("October", "Październik"): standalone form, capitalised. */
fun Month.title(locale: Locale): String = standalone(locale).replaceFirstChar { it.titlecase(locale) }

/** A month on its own inside a sentence ("your October", "Twój październik"). */
fun Month.standalone(locale: Locale): String = getDisplayName(TextStyle.FULL_STANDALONE, locale)

/** A notebook day: "Thursday 23 July 2026", "Czwartek 23 lipca 2026". */
fun LocalDate.longTitle(locale: Locale): String =
    format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

/** A month and year inside a sentence: "July 2026", and "lipca 2026" (genitive) in Polish. */
fun LocalDate.monthYear(locale: Locale): String = format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
