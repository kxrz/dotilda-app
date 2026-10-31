package com.dotilda.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A series of questions, one a day. Finishing it turns its days into a constellation.
 * Journeys live in assets/journeys.json; their questions are copied into the bank (with their
 * journeyId) so entries can point to them, and are never offered outside their journey.
 */
@Serializable
data class Journey(
    val id: String,
    val title: String,
    val description: String,
    val constellation: String,
    val questions: List<Question>,
) {
    val length: Int get() = questions.size
}

/** The journey under way, since [startedAt] (ISO date). */
@Serializable
data class ActiveJourney(val journeyId: String, val startedAt: String)

/** A finished journey: its constellation, the days it lit. */
@Serializable
data class Constellation(val journeyId: String, val name: String, val dates: List<String>)

@Serializable
private class JourneyFile(val journeys: List<Journey>)

private val json = Json { ignoreUnknownKeys = true }

fun loadJourneys(context: Context): List<Journey> {
    val text = readLocalizedAsset(context, "journeys")
    return json.decodeFromString(JourneyFile.serializer(), text).journeys.map { journey ->
        journey.copy(questions = journey.questions.map { it.copy(journeyId = journey.id) })
    }
}
