package com.streak.app.domain.repository

import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitLog
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.LogStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun observeHabitsWithStreak(): Flow<List<HabitWithStreak>>
    fun observeHabit(id: String): Flow<HabitWithStreak?>
    suspend fun getHabitsOnce(): List<Habit>
    suspend fun upsertHabit(habit: Habit)
    suspend fun deleteHabit(id: String)
    suspend fun setLogStatus(habitId: String, date: LocalDate, status: LogStatus?)
    suspend fun getLogsBetween(start: LocalDate, end: LocalDate): List<HabitLog>
    suspend fun markMissedFor(date: LocalDate)
}
