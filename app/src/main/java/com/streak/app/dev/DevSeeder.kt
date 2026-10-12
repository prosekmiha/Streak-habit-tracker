package com.streak.app.dev

import androidx.room.withTransaction
import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.local.dao.BackupDao
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.gamification.GameEvents
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSchedule
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.theme.HabitColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

// Generator testnih podatkov za meritve hitrosti (privzeto 200 navad, 5 let zapisov).
// Na voljo le v gradnjah debug in benchmark (BuildConfig.SEED_ENABLED); v release ga R8
// odstrani. Zbrise vse obstojece podatke.
@Singleton
class DevSeeder @Inject constructor(
    private val db: StreakDatabase,
    private val dao: BackupDao,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock,
    private val gameEvents: GameEvents
) {
    // Tiho kot uvoz: osvojene znacke se le zapomnijo, brez praznovanj
    suspend fun seed(habitCount: Int = 200, years: Int = 5) = gameEvents.quietly { seedLocked(habitCount, years) }

    private suspend fun seedLocked(habitCount: Int, years: Int) = withContext(Dispatchers.IO) {
        val today = dayClock.today()
        val first = today.minusYears(years.toLong())
        val random = Random(42)
        val zone = dayClock.zone()
        val habits = (0 until habitCount).map { i -> habit(i, first.plusDays(random.nextLong(30)), random) }

        db.withTransaction {
            dao.clearAll()
            dao.insertHabits(habits.map { it.toEntity() })
            habits.forEach { habit ->
                val completions = ArrayList<Completion>()
                val sessions = ArrayList<HabitSession>()
                var day = habit.startDate
                while (!day.isAfter(today)) {
                    if (scheduled(habit, day, random) && random.nextFloat() < DONE_RATE) {
                        val at = day.atTime(LocalTime.of(6 + random.nextInt(16), random.nextInt(60))).atZone(zone)
                        val partial = habit.goalType == GoalType.QUANTITY && random.nextFloat() < 0.15f
                        completions += Completion(
                            id = "${habit.id}-${day.toEpochDay()}",
                            habitId = habit.id,
                            date = day,
                            status = if (partial) CompletionStatus.PARTIAL else CompletionStatus.DONE,
                            value = if (partial) habit.targetValue / 2 else habit.targetValue,
                            loggedAt = at.toInstant(),
                            zoneOffset = at.offset ?: ZoneOffset.UTC,
                            note = if (random.nextFloat() < 0.02f) "Note ${day.dayOfMonth}" else null
                        )
                        if (habit.goalType == GoalType.DURATION) {
                            sessions += HabitSession(
                                id = "${habit.id}-s-${day.toEpochDay()}",
                                habitId = habit.id,
                                date = day,
                                startedAt = at.toInstant(),
                                endedAt = at.toInstant().plusSeconds(habit.targetValue.toLong()),
                                durationSeconds = habit.targetValue.toLong()
                            )
                        }
                    }
                    day = day.plusDays(1)
                }
                completions.chunked(CHUNK).forEach { chunk -> dao.insertCompletions(chunk.map { it.toEntity() }) }
                sessions.chunked(CHUNK).forEach { chunk -> dao.insertSessions(chunk.map { it.toEntity() }) }
            }
        }
        habitRepository.recalculateAll()
        settingsRepository.update { it.copy(onboardingDone = true, userName = "Benchmark", lastKnownDate = today) }
    }

    private fun habit(i: Int, start: LocalDate, random: Random): Habit {
        val goal = when (i % 5) {
            3 -> GoalType.QUANTITY
            4 -> GoalType.DURATION
            else -> GoalType.YES_NO
        }
        val schedule = when (i % 7) {
            5 -> HabitSchedule(ScheduleType.WEEKDAYS, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
            6 -> HabitSchedule(ScheduleType.TIMES_PER_WEEK, timesPerPeriod = 3)
            else -> HabitSchedule.Daily
        }
        val icon = IconMap.all[i % IconMap.all.size]
        return Habit(
            id = "seed-$i",
            title = "Habit ${i + 1}",
            icon = icon.name,
            colorHex = HabitColors[random.nextInt(HabitColors.size)],
            goalType = goal,
            targetValue = when (goal) {
                GoalType.YES_NO -> 1.0
                GoalType.QUIT -> 0.0
                GoalType.QUANTITY -> 8.0
                GoalType.DURATION -> 600.0
            },
            unit = if (goal == GoalType.QUANTITY) "glasses" else null,
            schedule = schedule,
            timeOfDay = TimeOfDay.entries[i % TimeOfDay.entries.size],
            startDate = start,
            createdAt = start.atStartOfDay(dayClock.zone()).toInstant().toEpochMilli(),
            sortOrder = i
        )
    }

    private fun scheduled(habit: Habit, day: LocalDate, random: Random): Boolean = when (habit.schedule.type) {
        ScheduleType.DAILY -> true
        ScheduleType.WEEKDAYS -> day.dayOfWeek in habit.schedule.weekdays
        ScheduleType.TIMES_PER_WEEK, ScheduleType.TIMES_PER_MONTH -> random.nextFloat() < 0.45f
    }

    private companion object {
        const val DONE_RATE = 0.78f
        const val CHUNK = 2_000
    }
}
