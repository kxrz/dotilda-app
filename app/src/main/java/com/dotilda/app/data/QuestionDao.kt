package com.dotilda.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface QuestionDao {
    @Upsert
    suspend fun upsertAll(questions: List<Question>)

    @Query("SELECT COUNT(*) FROM question")
    suspend fun count(): Int

    @Query("SELECT * FROM question ORDER BY RANDOM() LIMIT 1")
    suspend fun random(): Question?
}
