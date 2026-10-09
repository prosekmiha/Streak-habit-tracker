package com.streak.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.data.local.DatabaseGuard
import com.streak.app.data.local.DatabaseHealth
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.ClockSanity
import com.streak.app.domain.time.ClockStatus
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.LogResult
import com.streak.app.domain.usecase.ToggleHabitUseCase
import com.streak.app.ui.components.HabitCardUi
import com.streak.app.ui.components.WeekDayStatus
import com.streak.app.ui.components.WeekDayUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val dbHealth: DatabaseHealth = DatabaseHealth.Ok
) {
    val isToday: Boolean get() = selectedDate == today
    val allDone: Boolean get() = dueCount > 0 && doneCount >= dueCount
    fun item(habitId: String): HabitCardUi? = sections.flatMap { it.items }.firstOrNull { it.habit.id == habitId }
}

sealed interface HomeEvent {
    data object FutureBlocked : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val dayClock: DayClock,
    private val logHabit: ToggleHabitUseCase,
    databaseGuard: DatabaseGuard
) : ViewModel() {

    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    // Akcija, ki caka na potrditev zaradi skoka ure
    private val _pendingClockAction = MutableStateFlow<(suspend () -> Unit)?>(null)
    val hasPendingClockAction: StateFlow<Boolean> = _pendingClockAction.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val state: StateFlow<HomeUiState> = combine(
        habitRepository.observeHabits(),
        settingsRepository.settings,
        dayClock.todayFlow,
        selectedDate,
        databaseGuard.health
    ) { habits, settings, today, selected, health ->
        val date = selected?.takeIf { !it.isAfter(today.plusDays(FUTURE_DAYS)) && !it.isBefore(today.minusDays(PAST_DAYS)) } ?: today
        val cards = habits.mapNotNull { it.toCard(date) }
        val due = cards.count { it.state.isDue() }
        HomeUiState(
            loaded = true,
            userName = settings.userName,
            greeting = greetingFor(LocalTime.now()),
            today = today,
            selectedDate = date,
            weekDays = weekDays(habits, today),
            sections = sections(cards),
            dueCount = due,
            doneCount = cards.count { it.isDone },
            hasHabits = habits.isNotEmpty(),
            clockStatus = ClockSanity.check(settings.lastKnownDate, today),
            dbHealth = health
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

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
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun backToToday() { selectedDate.value = null }

    // Dotik kartice: da/ne preklopi, kolicina +1, opravljena kartica se odznaci
    fun onTap(item: HabitCardUi) = guarded {
        val date = state.value.selectedDate
        val result = when {
            item.isDone -> logHabit(item.habit, date)
            item.habit.goalType == GoalType.QUANTITY -> logHabit.addProgress(item.habit, date, 1.0)
            else -> logHabit(item.habit, date)
        }
        report(result)
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
        viewModelScope.launch { habitRepository.setArchived(habit.id, true) }
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

    fun dismissClockDialog() { _pendingClockAction.value = null }

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

    private fun HabitWithStreak.toCard(date: LocalDate): HabitCardUi? {
        val dayState = stateOn(date) ?: return null
        val hasRecord = completionOn(date) != null
        // Navade izven urnika (npr. ob vikendih) pokazemo le, ce imajo zapis
        if (dayState == DayState.INACTIVE && !hasRecord) return null
        if (dayState == DayState.NOT_SCHEDULED && habit.schedule.streakUnit == StreakUnit.DAY && !hasRecord) return null
        return HabitCardUi(habit, dayState, completionOn(date)?.value ?: 0.0, streak.currentStreak)
    }

    private fun DayState.isDue() = this == DayState.DONE || this == DayState.PARTIAL ||
        this == DayState.PENDING || this == DayState.MISSED

    // Ena mreza kot v dizajnu, razvrscena po delu dneva (jutro, popoldne, vecer, kadarkoli).
    // Naslove razdelkov pokazemo le, ce ima vsaka skupina vsaj dve navadi - sicer bi mreza razpadla.
    private fun sections(cards: List<HabitCardUi>): List<HabitSection> {
        if (cards.isEmpty()) return emptyList()
        val groups = DayOrder.mapNotNull { tod -> cards.filter { it.habit.timeOfDay == tod }.takeIf { it.isNotEmpty() }?.let { tod to it } }
        return if (groups.size > 1 && groups.all { it.second.size >= 2 }) groups.map { (tod, items) -> HabitSection(tod, items) }
        else listOf(HabitSection(null, groups.flatMap { it.second }))
    }

    private fun weekDays(habits: List<HabitWithStreak>, today: LocalDate): List<WeekDayUi> =
        (-PAST_DAYS..FUTURE_DAYS).map { offset ->
            val date = today.plusDays(offset)
            val states = habits.mapNotNull { it.stateOn(date) }
            val due = states.count { it == DayState.DONE || it == DayState.PARTIAL || it == DayState.MISSED || it == DayState.PENDING }
            val done = states.count { it == DayState.DONE }
            val status = when {
                date.isAfter(today) -> WeekDayStatus.FUTURE
                due > 0 && done >= due -> WeekDayStatus.ALL_DONE
                done > 0 -> WeekDayStatus.SOME_DONE
                else -> WeekDayStatus.NONE
            }
            WeekDayUi(date, status, date == today)
        }

    private fun greetingFor(time: LocalTime): Greeting = when (time.hour) {
        in 5..11 -> Greeting.MORNING
        in 12..17 -> Greeting.DAY
        else -> Greeting.EVENING
    }

    private companion object {
        val DayOrder = listOf(TimeOfDay.MORNING, TimeOfDay.AFTERNOON, TimeOfDay.EVENING, TimeOfDay.ANYTIME)
        const val PAST_DAYS = 13L
        const val FUTURE_DAYS = 6L
    }
}
