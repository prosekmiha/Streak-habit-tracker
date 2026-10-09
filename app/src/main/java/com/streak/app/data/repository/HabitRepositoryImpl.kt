package com.streak.app.data.repository

import com.streak.app.data.local.dao.HabitDao
import com.streak.app.data.local.dao.HabitLogDao
import com.streak.app.data.local.entity.HabitLogEntity
import com.streak.app.data.mapper.toDomain
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitLog
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.LogStatus
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.usecase.ComputeStreakUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val computeStreak: ComputeStreakUseCase
) : HabitRepository {

    override fun observeHabitsWithStreak(): Flow<List<HabitWithStreak>> =
        habitDao.observeHabitsWithLogs().map { list ->
            list.map { hwl ->
                val habit = hwl.habit.toDomain()
                val logs = hwl.logs.map { it.toDomain() }
                HabitWithStreak(habit, logs, computeStreak(habit, logs))
            }
        }

    override fun observeHabit(id: String): Flow<HabitWithStreak?> =
        habitDao.observeHabitWithLogs(id).map { hwl ->
            hwl?.let {
                val habit = it.habit.toDomain()
                val logs = it.logs.map { l -> l.toDomain() }
                HabitWithStreak(habit, logs, computeStreak(habit, logs))
            }
        }

    override suspend fun getHabitsOnce(): List<Habit> =
        habitDao.getHabitsOnce().map { it.toDomain() }

    override suspend fun upsertHabit(habit: Habit) = habitDao.upsert(habit.toEntity())

    override suspend fun deleteHabit(id: String) = habitDao.delete(id)

    override suspend fun setLogStatus(habitId: String, date: LocalDate, status: LogStatus?) {
        val epochDay = date.toEpochDay()
        if (status == null) {
            logDao.deleteForDay(habitId, epochDay)
        } else {
            val existing = logDao.getForDay(habitId, epochDay)
            logDao.upsert(
                HabitLogEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    habitId = habitId,
                    epochDay = epochDay,
                    status = status,
                    completedAt = if (status == LogStatus.DONE) System.currentTimeMillis() else null,
                    note = existing?.note
                )
            )
        }
    }

    override suspend fun getLogsBetween(start: LocalDate, end: LocalDate): List<HabitLog> =
        logDao.getBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }

    override suspend fun markMissedFor(date: LocalDate) {
        val epochDay = date.toEpochDay()
        val habits = habitDao.getHabitsOnce()
        habits.forEach { h ->
            val existing = logDao.getForDay(h.id, epochDay)
            if (existing == null) {
                logDao.upsert(
                    HabitLogEntity(
                        id = UUID.randomUUID().toString(),
                        habitId = h.id,
                        epochDay = epochDay,
                        status = LogStatus.MISSED,
                        completedAt = null,
                        note = null
                    )
                )
            }
        }
    }
}
