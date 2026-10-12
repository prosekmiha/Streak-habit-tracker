package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.streak.app.data.local.entity.CompletionEntity
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitReminderEntity
import com.streak.app.data.local.entity.MoodEntity
import com.streak.app.data.local.entity.SessionEntity
import com.streak.app.data.local.entity.TaskEntity
import com.streak.app.data.local.entity.VacationEntity

// Branje vseh podatkov in atomarna zamenjava (uvoz varnostne kopije, brisanje vsega)
@Dao
interface BackupDao {
    @Query("SELECT * FROM habits") suspend fun habits(): List<HabitEntity>
    @Query("SELECT * FROM habit_reminders") suspend fun reminders(): List<HabitReminderEntity>
    @Query("SELECT * FROM completions") suspend fun completions(): List<CompletionEntity>
    @Query("SELECT * FROM sessions") suspend fun sessions(): List<SessionEntity>
    @Query("SELECT * FROM vacations") suspend fun vacations(): List<VacationEntity>
    @Query("SELECT * FROM tasks") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM mood_entries") suspend fun moods(): List<MoodEntity>

    @Query("SELECT COUNT(*) FROM habits") suspend fun habitCount(): Int

    @Query("DELETE FROM habits") suspend fun clearHabits()
    @Query("DELETE FROM habit_reminders") suspend fun clearReminders()
    @Query("DELETE FROM completions") suspend fun clearCompletions()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM vacations") suspend fun clearVacations()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM mood_entries") suspend fun clearMoods()
    @Query("DELETE FROM streak_cache") suspend fun clearStreakCache()
    @Query("DELETE FROM daily_aggregates") suspend fun clearAggregates()

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertHabits(items: List<HabitEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertReminders(items: List<HabitReminderEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCompletions(items: List<CompletionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSessions(items: List<SessionEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertVacations(items: List<VacationEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTasks(items: List<TaskEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMoods(items: List<MoodEntity>)

    @Transaction
    suspend fun clearAll() {
        // Najprej odvisne tabele (tuji kljuci)
        clearReminders()
        clearCompletions()
        clearSessions()
        clearStreakCache()
        clearAggregates()
        clearHabits()
        clearVacations()
        clearTasks()
        clearMoods()
    }

    // Vse ali nic: ob napaki (npr. podvojen kljuc) ostanejo stari podatki
    @Transaction
    suspend fun replaceAll(
        habits: List<HabitEntity>,
        reminders: List<HabitReminderEntity>,
        completions: List<CompletionEntity>,
        sessions: List<SessionEntity>,
        vacations: List<VacationEntity>,
        tasks: List<TaskEntity>,
        moods: List<MoodEntity>
    ) {
        clearAll()
        insertHabits(habits)
        insertReminders(reminders)
        insertCompletions(completions)
        insertSessions(sessions)
        insertVacations(vacations)
        insertTasks(tasks)
        insertMoods(moods)
    }
}
