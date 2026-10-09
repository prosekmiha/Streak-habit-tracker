package com.streak.app.data.repository

import com.streak.app.data.local.StreakDatabase
import com.streak.app.data.mapper.reminderEntities
import com.streak.app.data.mapper.toDomain
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.Vacation
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.streak.DayEvaluator
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ComputeStreakUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class HabitRepositoryImpl @Inject constructor(
    private val db: StreakDatabase,
    private val recalculator: StreakRecalculator,
    private val computeStreak: ComputeStreakUseCase,
    private val dayClock: DayClock,
    private val settingsRepository: SettingsRepository
) : HabitRepository {

    private val habitDao get() = db.habitDao()
    private val completionDao get() = db.completionDao()
    private val statsDao get() = db.statsDao()
    private val vacationDao get() = db.vacationDao()

    override fun observeHabits(): Flow<List<HabitWithStreak>> =
        dayClock.todayFlow.flatMapLatest { today ->
            val from = today.minusDays(RECENT_PAST_DAYS)
            val to = today.plusDays(RECENT_FUTURE_DAYS)
            combine(
                habitDao.observeActive(),
                habitDao.observeAllReminders(),
                completionDao.observeBetween(from.toEpochDay(), today.toEpochDay()),
                statsDao.observeStreaks(),
                vacationDao.observeAll()
            ) { habits, reminders, completions, streaks, vacationEntities ->
                val remindersByHabit = reminders.groupBy { it.habitId }
                val completionsByHabit = completions.map { it.toDomain() }.groupBy { it.habitId }
                val streakByHabit = streaks.associateBy { it.habitId }
                val vacations = vacationEntities.map { it.toDomain() }
                habits.map { entity ->
                    val habit = entity.toDomain(remindersByHabit[entity.id].orEmpty())
                    val byDate = completionsByHabit[habit.id].orEmpty().associateBy { it.date }
                    HabitWithStreak(
                        habit = habit,
                        completions = byDate,
                        days = dayStates(habit, byDate, vacations, from, to, today),
                        streak = streakByHabit[habit.id]?.toDomain()
                            ?: HabitStreak.Empty.copy(unit = habit.schedule.streakUnit),
                        today = today
                    )
                }
            }
        }

    override fun observeArchivedHabits(): Flow<List<Habit>> =
        combine(habitDao.observeArchived(), habitDao.observeAllReminders()) { habits, reminders ->
            val byHabit = reminders.groupBy { it.habitId }
            habits.map { it.toDomain(byHabit[it.id].orEmpty()) }
        }

    override fun observeHabit(id: String): Flow<HabitWithStreak?> =
        combine(
            habitDao.observeById(id),
            habitDao.observeReminders(id),
            completionDao.observeForHabit(id),
            vacationDao.observeAll(),
            combine(dayClock.todayFlow, settingsRepository.settings) { today, s -> today to s.firstDayOfWeek }
        ) { entity, reminders, completions, vacationEntities, (today, firstDay) ->
            entity ?: return@combine null
            val habit = entity.toDomain(reminders)
            val byDate = completions.map { it.toDomain() }.associateBy { it.date }
            val vacations = vacationEntities.map { it.toDomain() }
            val from = listOfNotNull(habit.startDate, byDate.keys.minOrNull()).min()
            HabitWithStreak(
                habit = habit,
                completions = byDate,
                days = dayStates(habit, byDate, vacations, minOf(from, today), today.plusDays(RECENT_FUTURE_DAYS), today),
                streak = computeStreak(habit, byDate, vacations, today, firstDay),
                today = today
            )
        }

    override suspend fun getHabit(id: String): Habit? =
        habitDao.getById(id)?.toDomain(habitDao.getReminders(id))

    override suspend fun getHabits(includeArchived: Boolean): List<Habit> {
        val reminders = habitDao.getAllReminders().groupBy { it.habitId }
        val entities = if (includeArchived) habitDao.getAll() else habitDao.getActive()
        return entities.map { it.toDomain(reminders[it.id].orEmpty()) }
    }

    override suspend fun upsertHabit(habit: Habit) {
        val previousStart = habitDao.getById(habit.id)?.startEpochDay?.let { LocalDate.ofEpochDay(it) }
        habitDao.upsertWithReminders(habit.toEntity(), habit.reminderEntities())
        recalculator.onHabitChanged(habit.id, listOfNotNull(previousStart, habit.startDate).min())
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        val entity = habitDao.getById(id) ?: return
        habitDao.setArchivedAt(id, if (archived) dayClock.now().toEpochMilli() else null)
        recalculator.onHabitChanged(id, LocalDate.ofEpochDay(entity.startEpochDay))
    }

    override suspend fun deleteHabit(id: String) {
        val entity = habitDao.getById(id) ?: return
        habitDao.delete(id)
        recalculator.onHabitChanged(id, LocalDate.ofEpochDay(entity.startEpochDay))
    }

    override suspend fun getCompletion(habitId: String, date: LocalDate): Completion? =
        completionDao.get(habitId, date.toEpochDay())?.toDomain()

    override suspend fun saveCompletion(completion: Completion) {
        completionDao.upsert(completion.toEntity())
        recalculator.onCompletionChanged(completion.habitId, completion.date)
    }

    override suspend fun deleteCompletion(habitId: String, date: LocalDate) {
        completionDao.delete(habitId, date.toEpochDay())
        recalculator.onCompletionChanged(habitId, date)
    }

    override suspend fun getCompletionsBetween(start: LocalDate, end: LocalDate): List<Completion> =
        completionDao.getBetween(start.toEpochDay(), end.toEpochDay()).map { it.toDomain() }

    override fun observeCompletionsBetween(start: LocalDate, end: LocalDate): Flow<List<Completion>> =
        completionDao.observeBetween(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override fun observeAggregates(start: LocalDate, end: LocalDate): Flow<List<DailyAggregate>> =
        statsDao.observeAggregates(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override fun observeVacations(): Flow<List<Vacation>> =
        vacationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addVacation(vacation: Vacation) {
        vacationDao.insert(vacation.toEntity())
        recalculator.recalculateAll()
    }

    override suspend fun deleteVacation(id: Long) {
        vacationDao.delete(id)
        recalculator.recalculateAll()
    }

    override suspend fun saveSession(session: HabitSession) {
        db.sessionDao().upsert(session.toEntity())
    }

    override fun observeSessions(habitId: String): Flow<List<HabitSession>> =
        db.sessionDao().observeForHabit(habitId).map { list -> list.map { it.toDomain() } }

    override suspend fun recalculateAll() = recalculator.recalculateAll()

    private fun dayStates(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        from: LocalDate,
        to: LocalDate,
        today: LocalDate
    ): Map<LocalDate, DayState> {
        val result = LinkedHashMap<LocalDate, DayState>()
        var day = from
        while (!day.isAfter(to)) {
            result[day] = DayEvaluator.stateOn(habit, day, completions[day], vacations, today)
            day = day.plusDays(1)
        }
        return result
    }

    private companion object {
        // Okno za domaci zaslon: tedenski trak, pike zadnjih dni, mesecni pregled
        const val RECENT_PAST_DAYS = 35L
        const val RECENT_FUTURE_DAYS = 6L
    }
}
