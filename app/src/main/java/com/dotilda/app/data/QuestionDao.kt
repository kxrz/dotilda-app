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

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun byId(id: String): Question?

    @Query("SELECT * FROM question")
    suspend fun all(): List<Question>
}
