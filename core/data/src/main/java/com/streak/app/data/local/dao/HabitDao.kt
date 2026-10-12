package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archivedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE archivedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getActive(): List<HabitEntity>

    // @Upsert namesto REPLACE: REPLACE bi vrstico izbrisal in s tem prek
    // ON DELETE CASCADE izbrisal tudi vso zgodovino navade.
    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("UPDATE habits SET archivedAt = :archivedAt WHERE id = :id")
    suspend fun setArchivedAt(id: String, archivedAt: Long?)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    // --- Opomniki ---

    @Query("SELECT * FROM habit_reminders ORDER BY minuteOfDay ASC")
    fun observeAllReminders(): Flow<List<HabitReminderEntity>>

    @Query("SELECT * FROM habit_reminders WHERE habitId = :habitId ORDER BY minuteOfDay ASC")
    fun observeReminders(habitId: String): Flow<List<HabitReminderEntity>>

    @Query("SELECT * FROM habit_reminders WHERE habitId = :habitId ORDER BY minuteOfDay ASC")
    suspend fun getReminders(habitId: String): List<HabitReminderEntity>

    @Query("SELECT * FROM habit_reminders ORDER BY minuteOfDay ASC")
    suspend fun getAllReminders(): List<HabitReminderEntity>

    @Query("DELETE FROM habit_reminders WHERE habitId = :habitId")
    suspend fun deleteReminders(habitId: String)

    @Insert
    suspend fun insertReminders(reminders: List<HabitReminderEntity>)

    @Transaction
    suspend fun upsertWithReminders(habit: HabitEntity, reminders: List<HabitReminderEntity>) {
        upsert(habit)
        deleteReminders(habit.id)
        if (reminders.isNotEmpty()) insertReminders(reminders)
    }
}
