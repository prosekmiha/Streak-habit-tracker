package com.streak.app.domain.repository

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.Vacation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    // Aktivne (nearhivirane) navade z zapisi za nedavno okno in streakom iz predpomnilnika
    fun observeHabits(): Flow<List<HabitWithStreak>>
    fun observeArchivedHabits(): Flow<List<Habit>>

    // Ena navada s celotno zgodovino
    fun observeHabit(id: String): Flow<HabitWithStreak?>

    suspend fun getHabit(id: String): Habit?
    suspend fun getHabits(includeArchived: Boolean = false): List<Habit>
    suspend fun upsertHabit(habit: Habit)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun deleteHabit(id: String)

    suspend fun getCompletion(habitId: String, date: LocalDate): Completion?
    suspend fun saveCompletion(completion: Completion)
    suspend fun deleteCompletion(habitId: String, date: LocalDate)
    suspend fun getCompletionsBetween(start: LocalDate, end: LocalDate): List<Completion>
    fun observeCompletionsBetween(start: LocalDate, end: LocalDate): Flow<List<Completion>>

    fun observeAggregates(start: LocalDate, end: LocalDate): Flow<List<DailyAggregate>>

    fun observeVacations(): Flow<List<Vacation>>
    suspend fun addVacation(vacation: Vacation)
    suspend fun deleteVacation(id: Long)

    suspend fun saveSession(session: HabitSession)
    fun observeSessions(habitId: String): Flow<List<HabitSession>>

    // Ponovno izracuna vse streake in dnevne povzetke ("Preracunaj vse")
    suspend fun recalculateAll()
}
