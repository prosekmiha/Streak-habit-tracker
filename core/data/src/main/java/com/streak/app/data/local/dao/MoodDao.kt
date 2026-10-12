package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streak.app.data.local.entity.MoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {

    @Query("SELECT * FROM mood_entries WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<MoodEntity?>

    @Query("SELECT * FROM mood_entries WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): MoodEntity?

    @Query("SELECT * FROM mood_entries WHERE epochDay BETWEEN :start AND :end ORDER BY epochDay ASC")
    fun observeRange(start: Long, end: Long): Flow<List<MoodEntity>>

    @Query("SELECT * FROM mood_entries ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<MoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MoodEntity)

    @Query("DELETE FROM mood_entries WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)
}
