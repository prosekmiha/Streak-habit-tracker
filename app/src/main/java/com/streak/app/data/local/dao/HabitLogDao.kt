package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.streak.app.data.local.entity.HabitLogEntity

@Dao
interface HabitLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteForDay(habitId: String, epochDay: Long)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(habitId: String, epochDay: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE epochDay BETWEEN :start AND :end")
    suspend fun getBetween(start: Long, end: Long): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    suspend fun getForHabit(habitId: String): List<HabitLogEntity>
}
