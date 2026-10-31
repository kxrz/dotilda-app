package com.dotilda.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("dotilda")

private val json = Json { ignoreUnknownKeys = true }
private val constellationList = ListSerializer(Constellation.serializer())

data class Settings(
    val welcomed: Boolean,
    val reminderOn: Boolean,
    /** Minutes after midnight. */
    val reminderMinutes: Int,
    /** Look for a new version on GitHub when the app starts: Dotilda's only network access. */
    val checkUpdates: Boolean,
)

/** What Dotilda already offered today, so the question survives an app restart. */
data class DayState(
    val date: String,
    val questionId: String?,
    val shown: List<String>,
    val changes: Int,
    val weatherOffered: Boolean,
)

class Prefs(private val context: Context) {
    private object Keys {
        val welcomed = booleanPreferencesKey("welcomed")
        val reminderOn = booleanPreferencesKey("reminder_on")
        val reminderMinutes = intPreferencesKey("reminder_minutes")
        val checkUpdates = booleanPreferencesKey("check_updates")
        val lastVersion = stringPreferencesKey("last_version")
        val timerEnd = longPreferencesKey("timer_end")
        val dayDate = stringPreferencesKey("day_date")
        val dayQuestion = stringPreferencesKey("day_question")
        val dayShown = stringPreferencesKey("day_shown")
        val dayChanges = intPreferencesKey("day_changes")
        val dayWeather = booleanPreferencesKey("day_weather")
        val activeJourney = stringPreferencesKey("active_journey")
        val constellations = stringPreferencesKey("constellations")
        val openedLetters = stringSetPreferencesKey("opened_letters")
        val dotSize = stringPreferencesKey("dot_size")
        val skyLayout = stringPreferencesKey("sky_layout")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun settingsNow(): Settings = settings.first()

    suspend fun setWelcomed() {
        context.dataStore.edit { it[Keys.welcomed] = true }
    }

    suspend fun setReminder(on: Boolean, minutes: Int) {
        context.dataStore.edit {
            it[Keys.reminderOn] = on
            it[Keys.reminderMinutes] = minutes
        }
    }

    suspend fun setCheckUpdates(on: Boolean) {
        context.dataStore.edit { it[Keys.checkUpdates] = on }
    }

    /** Records the running version; returns the previous one (null on a fresh install). */
    suspend fun swapLastVersion(version: String): String? {
        var previous: String? = null
        context.dataStore.edit {
            previous = it[Keys.lastVersion]
            it[Keys.lastVersion] = version
        }
        return previous
    }

    /** When the spira timer ends (epoch millis), or null when none is running. */
    val timerEnd: Flow<Long?> = context.dataStore.data.map { it[Keys.timerEnd] }

    suspend fun setTimerEnd(at: Long?) {
        context.dataStore.edit { if (at != null) it[Keys.timerEnd] = at else it.remove(Keys.timerEnd) }
    }

    /** The journey under way, if any. */
    val activeJourney: Flow<ActiveJourney?> = context.dataStore.data.map { p ->
        p[Keys.activeJourney]?.let { json.decodeFromString(ActiveJourney.serializer(), it) }
    }

    suspend fun setActiveJourney(active: ActiveJourney?) {
        context.dataStore.edit {
            if (active != null) it[Keys.activeJourney] = json.encodeToString(ActiveJourney.serializer(), active)
            else it.remove(Keys.activeJourney)
        }
    }

    suspend fun stopJourneyIfActive(journeyId: String) {
        context.dataStore.edit { p ->
            val active = p[Keys.activeJourney]?.let { json.decodeFromString(ActiveJourney.serializer(), it) }
            if (active?.journeyId == journeyId) p.remove(Keys.activeJourney)
        }
    }

    /** Every finished journey, oldest first. */
    val constellations: Flow<List<Constellation>> = context.dataStore.data.map { p ->
        p[Keys.constellations]?.let { json.decodeFromString(constellationList, it) }.orEmpty()
    }

    suspend fun addConstellations(added: List<Constellation>) {
        context.dataStore.edit {
            val current = it[Keys.constellations]?.let { s -> json.decodeFromString(constellationList, s) }.orEmpty()
            it[Keys.constellations] = json.encodeToString(constellationList, current + added)
        }
    }

    /** Keys ([Entry.key]) of the letters already opened on or after their date. */
    val openedLetters: Flow<Set<String>> = context.dataStore.data.map { it[Keys.openedLetters].orEmpty() }

    suspend fun markLetterOpened(key: String) {
        context.dataStore.edit { it[Keys.openedLetters] = it[Keys.openedLetters].orEmpty() + key }
    }

    /** The look of the dots and of the Galaxy. */
    val sky: Flow<SkyStyle> = context.dataStore.data.map { p ->
        SkyStyle(
            dots = DotSize.entries.firstOrNull { it.name == p[Keys.dotSize] } ?: DotSize.Classic,
            layout = SkyLayout.entries.firstOrNull { it.name == p[Keys.skyLayout] } ?: SkyLayout.Rows,
        )
    }

    suspend fun setSky(style: SkyStyle) {
        context.dataStore.edit {
            it[Keys.dotSize] = style.dots.name
            it[Keys.skyLayout] = style.layout.name
        }
    }

    suspend fun day(): DayState = context.dataStore.data.first().let {
        DayState(
            date = it[Keys.dayDate].orEmpty(),
            questionId = it[Keys.dayQuestion],
            shown = it[Keys.dayShown]?.split(',')?.filter(String::isNotEmpty).orEmpty(),
            changes = it[Keys.dayChanges] ?: 0,
            weatherOffered = it[Keys.dayWeather] ?: false,
        )
    }

    suspend fun saveDay(day: DayState) {
        context.dataStore.edit {
            it[Keys.dayDate] = day.date
            if (day.questionId != null) it[Keys.dayQuestion] = day.questionId else it.remove(Keys.dayQuestion)
            it[Keys.dayShown] = day.shown.joinToString(",")
            it[Keys.dayChanges] = day.changes
            it[Keys.dayWeather] = day.weatherOffered
        }
    }

    private fun Preferences.toSettings() = Settings(
        welcomed = this[Keys.welcomed] ?: false,
        reminderOn = this[Keys.reminderOn] ?: true,
        reminderMinutes = this[Keys.reminderMinutes] ?: DEFAULT_REMINDER_MINUTES,
        checkUpdates = this[Keys.checkUpdates] ?: true,
    )

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 20 * 60
    }
}
