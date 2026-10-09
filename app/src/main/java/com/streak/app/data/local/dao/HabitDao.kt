package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitWithLogsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Transaction
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeHabitsWithLogs(): Flow<List<HabitWithLogsEntity>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeHabitWithLogs(id: String): Flow<HabitWithLogsEntity?>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC")
    suspend fun getHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE reminderEnabled = 1 AND isActive = 1")
    suspend fun getHabitsWithReminders(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)
}
