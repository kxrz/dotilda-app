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
    /** Whether Dotilda has asked what to call the user (asked once, even to those who said nothing). */
    val nameAsked: Boolean,
)

/** What Dotilda already offered today, so the question survives an app restart. */
/** See [Prefs.asides]. */
data class Asides(
    val dismissed: Set<String> = emptySet(),
    val updateSeen: Pair<String, String>? = null,
    val updatedOn: Pair<String, String>? = null,
)

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
        val dismissedAsides = stringSetPreferencesKey("dismissed_asides")
        val name = stringPreferencesKey("name")
        val nameAsked = booleanPreferencesKey("name_asked")
        val updateSeen = stringPreferencesKey("update_seen")
        val updatedOn = stringPreferencesKey("updated_on")
        val skyLayout = stringPreferencesKey("sky_layout")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun settingsNow(): Settings = settings.first()

    /** Forgets every preference, as on a fresh install (used by the screenshot tests). */
    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }

    /** What Dotilda calls the user: a first name or a nickname, or "" if they'd rather not say. */
    val name: Flow<String> = context.dataStore.data.map { it[Keys.name].orEmpty() }

    suspend fun setName(name: String) {
        context.dataStore.edit {
            it[Keys.name] = name.trim().take(NAME_MAX)
            it[Keys.nameAsked] = true
        }
    }

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

    /**
     * Dotilda's one-line asides on Today: those closed with their ×, when a new version was first
     * announced and when the app was last updated (both "version|date"), so none lingers.
     */
    val asides: Flow<Asides> = context.dataStore.data.map { p ->
        Asides(
            dismissed = p[Keys.dismissedAsides].orEmpty(),
            updateSeen = p[Keys.updateSeen]?.split('|')?.takeIf { it.size == 2 }?.let { it[0] to it[1] },
            updatedOn = p[Keys.updatedOn]?.split('|')?.takeIf { it.size == 2 }?.let { it[0] to it[1] },
        )
    }

    suspend fun dismissAside(key: String) {
        context.dataStore.edit { it[Keys.dismissedAsides] = it[Keys.dismissedAsides].orEmpty() + key }
    }

    /** Remembers the first day a new version was announced (kept if already set for it). */
    suspend fun markUpdateSeen(version: String, date: String) {
        context.dataStore.edit {
            if (it[Keys.updateSeen]?.substringBefore('|') != version) it[Keys.updateSeen] = "$version|$date"
        }
    }

    suspend fun setUpdatedOn(version: String, date: String) {
        context.dataStore.edit { it[Keys.updatedOn] = "$version|$date" }
    }

    /** The look of the dots and of the Galaxy. */
    val sky: Flow<SkyStyle> = context.dataStore.data.map { p ->
        SkyStyle(
            dots = DotSize.entries.firstOrNull { it.name == p[Keys.dotSize] } ?: DotSize.Classic,
            layout = SkyLayout.entries.firstOrNull { it.name == p[Keys.skyLayout] } ?: SkyLayout.Rows,
        )
    }

    // One key at a time: two quick taps must not overwrite each other's choice.
    suspend fun setDotSize(size: DotSize) {
        context.dataStore.edit { it[Keys.dotSize] = size.name }
    }

    suspend fun setSkyLayout(layout: SkyLayout) {
        context.dataStore.edit { it[Keys.skyLayout] = layout.name }
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
        nameAsked = this[Keys.nameAsked] ?: false,
    )

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 20 * 60
        const val NAME_MAX = 24
    }
}
