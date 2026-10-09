package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streak.app.data.local.entity.CompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    // REPLACE je tu varen: completions nima podrejenih tabel, unikatni indeks
    // (habitId, epochDay) pa zagotovi en zapis na navado in dan.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(completions: List<CompletionEntity>)

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun get(habitId: String, epochDay: Long): CompletionEntity?

    @Query("DELETE FROM completions WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun delete(habitId: String, epochDay: Long)

    @Query("SELECT * FROM completions WHERE epochDay BETWEEN :start AND :end")
    fun observeBetween(start: Long, end: Long): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE epochDay BETWEEN :start AND :end")
    suspend fun getBetween(start: Long, end: Long): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId ORDER BY epochDay ASC")
    fun observeForHabit(habitId: String): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE habitId = :habitId ORDER BY epochDay ASC")
    suspend fun getForHabit(habitId: String): List<CompletionEntity>

    @Query("SELECT * FROM completions")
    suspend fun getAll(): List<CompletionEntity>
}
