package com.streak.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.backup.BackupReminder
import com.streak.app.domain.backup.BackupService
import com.streak.app.domain.backup.SnapshotInfo
import com.streak.app.domain.health.DatabaseHealth
import com.streak.app.domain.health.DatabaseHealthMonitor
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.session.SessionController
import com.streak.app.domain.time.ClockSanity
import com.streak.app.domain.time.ClockStatus
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.LogResult
import com.streak.app.domain.usecase.SetHabitArchivedUseCase
import com.streak.app.domain.usecase.ToggleHabitUseCase
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.WeekDayUi
import com.streak.app.ui.home.HomeUiMapping.isDue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class Greeting { MORNING, DAY, EVENING }

data class HabitSection(val timeOfDay: TimeOfDay?, val items: List<HabitCardUi>)

data class HomeUiState(
    val loaded: Boolean = false,
    val userName: String = "",
    val greeting: Greeting = Greeting.MORNING,
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDays: List<WeekDayUi> = emptyList(),
    val sections: List<HabitSection> = emptyList(),
    val dueCount: Int = 0,
    val doneCount: Int = 0,
    val hasHabits: Boolean = false,
    val clockStatus: ClockStatus = ClockStatus.Ok,
    val dbHealth: DatabaseHealth = DatabaseHealth.Ok,
    // Najdaljsa trenutna veriga med navadami (za cip ob traku napredka)
    val topStreak: Int = 0,
    val celebration: Celebration? = null,
    // Cas za varnostno kopijo (30 dni)
    val backupDue: Boolean = false
) {
    val isToday: Boolean get() = selectedDate == today
    val allDone: Boolean get() = dueCount > 0 && doneCount >= dueCount
    fun item(habitId: String): HabitCardUi? = sections.flatMap { it.items }.firstOrNull { it.habit.id == habitId }
}

sealed interface HomeEvent {
    data object FutureBlocked : HomeEvent
    data class OpenSession(val habitId: String) : HomeEvent
}

// Pasica tekoce seje na vrhu domacega zaslona
data class ActiveSessionBar(val habitId: String, val title: String, val running: Boolean, val elapsedSeconds: Long)

// Praznovanje mejnika (npr. 7 dni zapored)
data class Celebration(val habitTitle: String, val count: Int, val unit: StreakUnit)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock,
    private val logHabit: ToggleHabitUseCase,
    private val setArchived: SetHabitArchivedUseCase,
    private val sessionController: SessionController,
    private val backup: BackupService,
    databaseHealth: DatabaseHealthMonitor
) : ViewModel() {

    // Tekoca seja (osvezi se vsako sekundo, le ko je zaslon viden)
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSession: StateFlow<ActiveSessionBar?> = sessionController.active
        .flatMapLatest { s ->
            if (s == null) flowOf(null) else flow<ActiveSessionBar?> {
                while (true) {
                    emit(ActiveSessionBar(s.habitId, s.title, s.running, s.elapsedMs(sessionController.now()) / 1000))
                    if (!s.running) break
                    delay(1000)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val celebration = MutableStateFlow<Celebration?>(null)
    private val milestones = MilestoneTracker()
    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    // Akcija, ki caka na potrditev zaradi skoka ure
    private val _pendingClockAction = MutableStateFlow<(suspend () -> Unit)?>(null)
    val hasPendingClockAction: StateFlow<Boolean> = _pendingClockAction.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Prazna baza + samodejni posnetek (npr. nov telefon): ponudi obnovo
    private val _restoreOffer = MutableStateFlow<SnapshotInfo?>(null)
    val restoreOffer: StateFlow<SnapshotInfo?> = _restoreOffer.asStateFlow()

    val state: StateFlow<HomeUiState> = combine(
        habitRepository.observeHabits(),
        settingsRepository.settings,
        dayClock.todayFlow,
        combine(selectedDate, celebration) { d, c -> d to c },
        databaseHealth.health
    ) { habits, settings, today, (selected, celebrating), health ->
        milestones.check(habits)?.let { celebration.value = it }
        val date = HomeUiMapping.clampDate(selected, today)
        val cards = habits.mapNotNull { HomeUiMapping.card(it, date) }
        HomeUiState(
            loaded = true,
            userName = settings.userName,
            greeting = HomeUiMapping.greetingFor(LocalTime.now()),
            today = today,
            selectedDate = date,
            weekDays = HomeUiMapping.weekDays(habits, today),
            sections = HomeUiMapping.sections(cards),
            dueCount = cards.count { it.state.isDue() },
            doneCount = cards.count { it.isDone },
            hasHabits = habits.isNotEmpty(),
            clockStatus = ClockSanity.check(settings.lastKnownDate, today),
            dbHealth = health,
            topStreak = HomeUiMapping.topDailyStreak(habits),
            celebration = celebrating,
            backupDue = BackupReminder.isDue(
                now = Instant.now(),
                enabled = settings.backupReminderEnabled,
                lastBackup = settings.lastBackupAt,
                snoozedUntil = settings.backupReminderSnoozedUntil,
                oldestHabitCreatedAt = habits.minOfOrNull { it.habit.createdAt }?.let(Instant::ofEpochMilli)
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    init {
        // Zapomni si zadnji znani dan, ce se ura ni nenavadno premaknila
        viewModelScope.launch {
            dayClock.todayFlow.collect { today ->
                val settings = settingsRepository.settings.first()
                if (ClockSanity.check(settings.lastKnownDate, today) == ClockStatus.Ok && settings.lastKnownDate != today) {
                    settingsRepository.update { it.copy(lastKnownDate = today) }
                }
            }
        }
        viewModelScope.launch { _restoreOffer.value = backup.pendingSnapshot() }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun backToToday() {
        selectedDate.value = null
    }

    // Dotik kartice: da/ne preklopi, kolicina +1, casovna navada odpre sejo,
    // opravljena kartica se odznaci
    fun onTap(item: HabitCardUi) {
        if (!item.isDone && !item.isNeutral && item.habit.goalType == GoalType.DURATION && state.value.isToday) {
            viewModelScope.launch { events.send(HomeEvent.OpenSession(item.habit.id)) }
            return
        }
        guarded {
            val date = state.value.selectedDate
            if (!item.isDone) milestones.expect(item.habit.id, item.currentStreak)
            val result = when {
                item.isDone -> logHabit(item.habit, date)
                item.habit.goalType == GoalType.QUANTITY -> logHabit.addProgress(item.habit, date, 1.0)
                else -> logHabit(item.habit, date)
            }
            report(result)
        }
    }

    // Oznaci/odznaci brez odpiranja seje (iz menija dejanj)
    fun toggleDone(item: HabitCardUi) = guarded {
        if (!item.isDone) milestones.expect(item.habit.id, item.currentStreak)
        report(logHabit(item.habit, state.value.selectedDate))
    }

    fun dismissCelebration() {
        celebration.value = null
    }

    fun setStatus(habit: Habit, status: CompletionStatus?) = guarded {
        report(logHabit.setStatus(habit, state.value.selectedDate, status))
    }

    fun addProgress(habit: Habit, delta: Double) = guarded {
        report(logHabit.addProgress(habit, state.value.selectedDate, delta))
    }

    fun saveNote(habit: Habit, note: String) {
        viewModelScope.launch {
            val existing = habitRepository.getCompletion(habit.id, state.value.selectedDate) ?: return@launch
            habitRepository.saveCompletion(existing.copy(note = note.trim().ifBlank { null }))
        }
    }

    fun archive(habit: Habit) {
        viewModelScope.launch { setArchived(habit.id, archived = true) }
    }

    fun acceptRestore() {
        _restoreOffer.value = null
        viewModelScope.launch { backup.restoreSnapshot() }
    }

    fun declineRestore() {
        _restoreOffer.value = null
        viewModelScope.launch { backup.discardSnapshot() }
    }

    fun snoozeBackupReminder() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(backupReminderSnoozedUntil = Instant.now().plus(BackupReminder.SNOOZE)) }
        }
    }

    // Uporabnik potrdi, da je datum naprave pravilen
    fun confirmClock() {
        viewModelScope.launch {
            val today = dayClock.today()
            settingsRepository.update { it.copy(lastKnownDate = today) }
            _pendingClockAction.value?.invoke()
            _pendingClockAction.value = null
        }
    }

    fun dismissClockDialog() {
        _pendingClockAction.value = null
    }

    // Pred vpisom preveri skok ure; ob skoku pocaka na potrditev
    private fun guarded(action: suspend () -> Unit) {
        viewModelScope.launch {
            if (state.value.clockStatus != ClockStatus.Ok) _pendingClockAction.value = action
            else action()
        }
    }

    private suspend fun report(result: LogResult) {
        if (result == LogResult.FUTURE_BLOCKED) events.send(HomeEvent.FutureBlocked)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
