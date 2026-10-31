package com.dotilda.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private class QuestionFile(val questions: List<Question>)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Copies the bundled question bank, and the journeys' questions, into Room, in the interface's
 * language. Runs on every launch so an app update (or a change of language) can fix or add
 * questions; questions removed from the files stay in the database because past entries still
 * point to them.
 */
suspend fun syncQuestionBank(context: Context, dao: QuestionDao): Unit = withContext(Dispatchers.IO) {
    val text = readLocalizedAsset(context, "questions")
    dao.upsertAll(json.decodeFromString<QuestionFile>(text).questions + loadJourneys(context).flatMap { it.questions })
}
