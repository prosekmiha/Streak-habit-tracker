package com.streak.app.testing

import com.streak.app.domain.backup.BackupContents
import com.streak.app.domain.backup.BackupService
import com.streak.app.domain.backup.ImportMode
import com.streak.app.domain.backup.ParseResult
import com.streak.app.domain.backup.SnapshotInfo
import com.streak.app.domain.health.DatabaseHealth
import com.streak.app.domain.health.DatabaseHealthMonitor
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.MoodEntry
import com.streak.app.domain.model.Vacation
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.session.ActiveSession
import com.streak.app.domain.session.AmbientSound
import com.streak.app.domain.session.SessionController
import com.streak.app.domain.session.StartResult
import com.streak.app.domain.session.TimePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate

// Preprosti fake-i domenskih vmesnikov za teste use-case-ov in ViewModelov (brez mockk)

class FakeHabitRepository : HabitRepository {
    val habits = MutableStateFlow<Map<String, Habit>>(emptyMap())
    val completions = MutableStateFlow<Map<Pair<String, LocalDate>, Completion>>(emptyMap())

    // Kar vrne observeHabits(); test ga nastavi z withStreak(...)
    val withStreak = MutableStateFlow<List<HabitWithStreak>>(emptyList())

    override fun observeHabits(): Flow<List<HabitWithStreak>> = withStreak
    override fun observeArchivedHabits(): Flow<List<Habit>> = habits.map { all -> all.values.filter { it.isArchived } }
    override fun observeHabit(id: String): Flow<HabitWithStreak?> = withStreak.map { list -> list.firstOrNull { it.habit.id == id } }

    override suspend fun getHabit(id: String): Habit? = habits.value[id]
    override suspend fun getHabits(includeArchived: Boolean): List<Habit> =
        habits.value.values.filter { includeArchived || !it.isArchived }

    override suspend fun upsertHabit(habit: Habit) = habits.update { it + (habit.id to habit) }

    override suspend fun setArchived(id: String, archived: Boolean) = habits.update { all ->
        val habit = all[id] ?: return@update all
        all + (id to habit.copy(archivedAt = if (archived) 1L else null))
    }

    override suspend fun deleteHabit(id: String) = habits.update { it - id }

    override suspend fun getCompletion(habitId: String, date: LocalDate): Completion? = completions.value[habitId to date]
    override suspend fun saveCompletion(completion: Completion) =
        completions.update { it + ((completion.habitId to completion.date) to completion) }
    override suspend fun deleteCompletion(habitId: String, date: LocalDate) = completions.update { it - (habitId to date) }
    override suspend fun getCompletionsBetween(start: LocalDate, end: LocalDate): List<Completion> =
        completions.value.values.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
    override fun observeCompletionsBetween(start: LocalDate, end: LocalDate): Flow<List<Completion>> =
        completions.map { all -> all.values.filter { !it.date.isBefore(start) && !it.date.isAfter(end) } }

    override fun observeAggregates(start: LocalDate, end: LocalDate): Flow<List<DailyAggregate>> = MutableStateFlow(emptyList())
    override fun observeVacations(): Flow<List<Vacation>> = MutableStateFlow(emptyList())
    override suspend fun addVacation(vacation: Vacation) = Unit
    override suspend fun deleteVacation(id: Long) = Unit
    override suspend fun saveSession(session: HabitSession) = Unit
    override fun observeSessions(habitId: String): Flow<List<HabitSession>> = MutableStateFlow(emptyList())
    override suspend fun recalculateAll() = Unit
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    val current: AppSettings get() = state.value

    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
}

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<String>()
    val cancelled = mutableListOf<String>()
    var exact = true

    override fun canScheduleExact(): Boolean = exact
    override suspend fun rescheduleAll() = Unit
    override fun schedule(habit: Habit) {
        scheduled += habit.id
    }
    override fun cancel(habitId: String, includeSnooze: Boolean) {
        cancelled += habitId
    }
    override fun snooze(habitId: String, minutes: Long) = Unit
    override suspend fun scheduleDailyReset(settings: AppSettings?) = Unit
    override suspend fun scheduleDaily(settings: AppSettings?) = Unit
}

class FakeSessionController(private val clock: () -> TimePoint = { TimePoint(0, Instant.EPOCH, 0) }) : SessionController {
    private val state = MutableStateFlow<ActiveSession?>(null)
    override val active: StateFlow<ActiveSession?> = state.asStateFlow()
    var exactAllowed = true

    override fun now(): TimePoint = clock()
    override fun exactAlarmsAllowed(): Boolean = exactAllowed
    override suspend fun awaitLoaded() = Unit
    override suspend fun lastAmbient(): AmbientSound = AmbientSound.NONE
    override suspend fun restore() = Unit

    override suspend fun start(habit: Habit): StartResult {
        val current = state.value
        return when {
            current == null -> {
                state.value = ActiveSession.start(habit.id, habit.title, habit.targetSeconds(), now(), AmbientSound.NONE)
                StartResult.Started
            }
            current.habitId == habit.id -> StartResult.Resumed
            else -> StartResult.Conflict(current)
        }
    }

    override suspend fun togglePause() = state.update { s -> s?.let { if (it.running) it.pause(now()) else it.resume(now()) } }
    override suspend fun pause() = state.update { it?.pause(now()) }
    override suspend fun resume() = state.update { it?.resume(now()) }
    override suspend fun setAmbient(sound: AmbientSound) = state.update { it?.copy(ambient = sound) }
    override suspend fun onGoalReached() = Unit

    override suspend fun finish(save: Boolean): Long? {
        val s = state.value ?: return null
        state.value = null
        return if (save) s.elapsedMs(now()) / 1000 else null
    }
}

class FakeBackupService(var snapshot: SnapshotInfo? = null) : BackupService {
    var restored = 0
    var discarded = 0
    val imports = mutableListOf<ImportMode>()

    override suspend fun readContents() = BackupContents()
    override suspend fun exportJson(password: CharArray?): String = "{}"
    override suspend fun exportCsv(): String = ""
    override fun isEncrypted(text: String): Boolean = false
    override suspend fun parse(text: String, password: CharArray?): ParseResult = ParseResult.NotABackup
    override suspend fun import(incoming: BackupContents, mode: ImportMode) {
        imports += mode
    }
    override suspend fun pendingSnapshot(): SnapshotInfo? = snapshot
    override suspend fun restoreSnapshot(): Boolean {
        restored++
        return snapshot != null
    }
    override suspend fun discardSnapshot() {
        discarded++
    }
    override suspend fun writeSnapshot() = Unit
    override suspend fun wipeAll() = Unit
}

class FakeDatabaseHealthMonitor(initial: DatabaseHealth = DatabaseHealth.Ok) : DatabaseHealthMonitor {
    override val health = MutableStateFlow(initial)
}

// Navada s pripravljenimi stanji dni (kot jih vrne repozitorij)
fun withStreak(
    habit: Habit,
    today: LocalDate,
    days: Map<LocalDate, DayState> = mapOf(today to DayState.PENDING),
    completions: Map<LocalDate, Completion> = emptyMap(),
    streak: HabitStreak = HabitStreak.Empty
) = HabitWithStreak(habit = habit, completions = completions, days = days, streak = streak, today = today)

class FakeMoodRepository : MoodRepository {
    val entries = MutableStateFlow<Map<LocalDate, MoodEntry>>(emptyMap())

    override fun observe(date: LocalDate): Flow<MoodEntry?> = entries.map { it[date] }
    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<MoodEntry>> =
        entries.map { m -> m.values.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }.sortedBy { it.date } }
    override fun observeAll(): Flow<List<MoodEntry>> = entries.map { m -> m.values.sortedBy { it.date } }
    override suspend fun get(date: LocalDate): MoodEntry? = entries.value[date]
    override suspend fun save(entry: MoodEntry) { entries.value = entries.value + (entry.date to entry) }
    override suspend fun delete(date: LocalDate) { entries.value = entries.value - date }
}
