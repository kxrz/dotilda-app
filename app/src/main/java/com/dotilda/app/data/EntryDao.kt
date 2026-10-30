package com.dotilda.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry): Long

    @Query("SELECT * FROM entry WHERE date = :date ORDER BY createdAt LIMIT 1")
    fun firstOn(date: String): Flow<Entry?>

    @Query("SELECT * FROM entry WHERE date = :date ORDER BY createdAt LIMIT 1")
    suspend fun firstOnNow(date: String): Entry?

    @Query("SELECT * FROM entry WHERE date BETWEEN :from AND :to ORDER BY date, createdAt")
    fun between(from: String, to: String): Flow<List<Entry>>

    @Query("SELECT * FROM entry WHERE date >= :from ORDER BY date, createdAt")
    suspend fun since(from: String): List<Entry>

    @Query("SELECT * FROM entry WHERE date < :date ORDER BY date DESC, createdAt DESC LIMIT 1")
    suspend fun lastBefore(date: String): Entry?

    @Query("SELECT * FROM entry WHERE questionId = :questionId AND date < :date ORDER BY date DESC LIMIT 1")
    suspend fun previousAnswer(questionId: String, date: String): Entry?

    @Query("SELECT * FROM entry ORDER BY date, createdAt")
    fun all(): Flow<List<Entry>>

    @Query("SELECT MIN(date) FROM entry")
    suspend fun firstDate(): String?
}
