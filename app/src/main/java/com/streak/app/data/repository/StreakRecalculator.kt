package com.streak.app.data.repository

import androidx.room.withTransaction
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.local.entity.DailyAggregateEntity
import com.streak.app.data.mapper.toDomain
import com.streak.app.data.mapper.toCacheEntity
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.Vacation
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.streak.DayEvaluator
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ComputeStreakUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// Vzdrzuje predpomnilnik streakov (streak_cache) in dnevne povzetke
// (daily_aggregates), da UI in statistika ne berejo vseh surovih zapisov.
// Posodobitve so inkrementalne (ena navada / dolocen dan); recalculateAll je
// funkcija "Preracunaj vse" za popravilo.
@Singleton
class StreakRecalculator @Inject constructor(
    private val db: StreakDatabase,
    private val computeStreak: ComputeStreakUseCase,
    private val dayClock: DayClock,
    private val settingsRepository: SettingsRepository
) {
    private val mutex = Mutex()
    private val habitDao get() = db.habitDao()
    private val completionDao get() = db.completionDao()
    private val statsDao get() = db.statsDao()

    // Po spremembi zapisa navade na dolocen dan
    suspend fun onCompletionChanged(habitId: String, date: LocalDate) = mutex.withLock {
        val today = dayClock.today()
        recomputeHabitLocked(habitId, today)
        recomputeAggregatesLocked(date, date, today)
    }

    // Po spremembi navade (urnik, zacetek, arhiv) - povzetki za celotno obdobje navade
    suspend fun onHabitChanged(habitId: String, affectedFrom: LocalDate) = mutex.withLock {
        val today = dayClock.today()
        recomputeHabitLocked(habitId, today)
        if (!affectedFrom.isAfter(today)) recomputeAggregatesLocked(affectedFrom, today, today)
    }

    // Ob menjavi dneva: trenutni streak je odvisen od "danes"
    suspend fun onDayChanged() = mutex.withLock {
        val today = dayClock.today()
        val streaks = statsDao.getStreaks()
        val stale = streaks.filter { it.computedForEpochDay != today.toEpochDay() }
        val habits = habitDao.getActive()
        val missingCache = habits.map { it.id }.toSet() - streaks.map { it.habitId }.toSet()
        if (stale.isEmpty() && missingCache.isEmpty()) return@withLock
        val oldest = stale.minOfOrNull { LocalDate.ofEpochDay(it.computedForEpochDay) } ?: today
        recomputeAllStreaksLocked(today)
        recomputeAggregatesLocked(minOf(oldest, today), today, today)
    }

    suspend fun recalculateAll() = mutex.withLock {
        val today = dayClock.today()
        recomputeAllStreaksLocked(today)
        val start = habitDao.getAll().minOfOrNull { LocalDate.ofEpochDay(it.startEpochDay) }
        val firstCompletion = completionDao.getAll().minOfOrNull { it.epochDay }?.let { LocalDate.ofEpochDay(it) }
        val from = listOfNotNull(start, firstCompletion).minOrNull() ?: return@withLock
        recomputeAggregatesLocked(minOf(from, today), today, today)
    }

    private suspend fun recomputeHabitLocked(habitId: String, today: LocalDate) {
        val entity = habitDao.getById(habitId) ?: return
        val habit = entity.toDomain(habitDao.getReminders(habitId))
        val completions = completionDao.getForHabit(habitId).associate { LocalDate.ofEpochDay(it.epochDay) to it.toDomain() }
        val streak = computeStreak(habit, completions, vacations(), today, firstDayOfWeek())
        statsDao.upsertStreak(streak.toCacheEntity(habitId, today))
    }

    private suspend fun recomputeAllStreaksLocked(today: LocalDate) {
        val habits = loadHabits()
        val byHabit = completionDao.getAll().map { it.toDomain() }.groupBy { it.habitId }
        val vacations = vacations()
        val firstDay = firstDayOfWeek()
        val entities = habits.map { habit ->
            val completions = byHabit[habit.id].orEmpty().associateBy { it.date }
            computeStreak(habit, completions, vacations, today, firstDay).toCacheEntity(habit.id, today)
        }
        statsDao.upsertStreaks(entities)
    }

    // Povzetek za vsak dan v [from, to] cez vse navade
    private suspend fun recomputeAggregatesLocked(from: LocalDate, to: LocalDate, today: LocalDate) {
        val end = minOf(to, today)
        if (from.isAfter(end)) return
        val habits = loadHabits()
        val completions = completionDao.getBetween(from.toEpochDay(), end.toEpochDay())
            .map { it.toDomain() }
            .associateBy { it.habitId to it.date }
        val vacations = vacations()
        val entities = ArrayList<DailyAggregateEntity>()
        var day = from
        while (!day.isAfter(end)) {
            val (scheduled, done) = aggregateFor(day, habits, completions, vacations, today)
            if (scheduled > 0 || done > 0) entities += DailyAggregateEntity(day.toEpochDay(), scheduled, done)
            day = day.plusDays(1)
        }
        db.withTransaction {
            statsDao.replaceAggregates(from.toEpochDay(), end.toEpochDay(), entities)
        }
    }

    private fun aggregateFor(
        day: LocalDate,
        habits: List<Habit>,
        completions: Map<Pair<String, LocalDate>, Completion>,
        vacations: List<Vacation>,
        today: LocalDate
    ): Pair<Int, Int> {
        var scheduled = 0
        var done = 0
        for (habit in habits) {
            if (!wasTrackedOn(habit, day)) continue
            when (DayEvaluator.stateOn(habit, day, completions[habit.id to day], vacations, today)) {
                DayState.DONE -> { scheduled++; done++ }
                DayState.MISSED, DayState.PENDING, DayState.PARTIAL -> scheduled++
                else -> Unit
            }
        }
        return scheduled to done
    }

    // Arhivirana navada steje v povzetkih le do dneva arhiviranja
    private fun wasTrackedOn(habit: Habit, day: LocalDate): Boolean {
        val archivedAt = habit.archivedAt ?: return true
        val archivedDay = Instant.ofEpochMilli(archivedAt).atZone(dayClock.zone()).toLocalDate()
        return day.isBefore(archivedDay)
    }

    private suspend fun loadHabits(): List<Habit> {
        val reminders = habitDao.getAllReminders().groupBy { it.habitId }
        return habitDao.getAll().map { it.toDomain(reminders[it.id].orEmpty()) }
    }

    private suspend fun vacations(): List<Vacation> = db.vacationDao().getAll().map { it.toDomain() }

    private suspend fun firstDayOfWeek() = settingsRepository.settings.first().firstDayOfWeek
}
