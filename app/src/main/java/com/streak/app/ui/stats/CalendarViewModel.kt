package com.streak.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.streak.Periods
import com.streak.app.domain.streak.DayEvaluator
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class DailyTotals(val scheduled: Int = 0, val done: Int = 0) {
    val ratio: Float get() = if (scheduled == 0) 0f else done.toFloat() / scheduled
}

// Navada in njeno stanje za izbran dan
data class HabitDay(val habit: Habit, val state: DayState)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    // Tedenski povzetek: delez opravljenih ta in prejsnji teden
    val thisWeek: DailyTotals = DailyTotals(),
    val lastWeek: DailyTotals = DailyTotals(),
    val completionByDate: Map<LocalDate, Float> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val habitsForDate: List<HabitDay> = emptyList(),
    val tasksForDate: List<Task> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    taskRepository: TaskRepository,
    settingsRepository: SettingsRepository,
    dayClock: DayClock,
    private val toggleHabit: ToggleHabitUseCase
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    // Delez opravljenih navad po dnevih iz vnaprej izracunanih povzetkov
    private val completionByDate: Flow<Map<LocalDate, Float>> = month.flatMapLatest { m ->
        habitRepository.observeAggregates(m.atDay(1), m.atEndOfMonth())
            .map { list -> list.filter { it.done > 0 }.associate { it.date to it.ratio } }
    }

    private val habitsForDate: Flow<List<HabitDay>> = combine(selectedDate, dayClock.todayFlow) { d, t -> d to t }
        .flatMapLatest { (date, today) ->
            if (date == null) flowOf(emptyList())
            else combine(
                habitRepository.observeHabits(),
                habitRepository.observeCompletionsBetween(date, date),
                habitRepository.observeVacations()
            ) { habits, completions, vacations ->
                val byHabit = completions.associateBy { it.habitId }
                habits.map { it.habit }
                    .filter { it.isActiveOn(date) || byHabit.containsKey(it.id) }
                    .map { HabitDay(it, DayEvaluator.stateOn(it, date, byHabit[it.id], vacations, today)) }
            }
        }

    private val tasksForDate: Flow<List<Task>> = selectedDate.flatMapLatest { date ->
        if (date != null) taskRepository.observeTasksForDate(date) else flowOf(emptyList())
    }

    // Povzetek zadnjih dveh tednov iz dnevnih povzetkov
    private val weekly: Flow<Pair<DailyTotals, DailyTotals>> =
        combine(dayClock.todayFlow, settingsRepository.settings.map { it.firstDayOfWeek }.distinctUntilChanged()) { t, f -> t to f }
            .flatMapLatest { (today, firstDay) ->
                val thisStart = Periods.start(today, StreakUnit.WEEK, firstDay)
                val lastStart = thisStart.minusWeeks(1)
                habitRepository.observeAggregates(lastStart, today).map { list ->
                    fun sum(from: LocalDate, to: LocalDate) = list.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
                        .let { days -> DailyTotals(days.sumOf { it.scheduled }, days.sumOf { it.done }) }
                    sum(thisStart, today) to sum(lastStart, thisStart.minusDays(1))
                }
            }

    val state: StateFlow<CalendarUiState> = combine(
        combine(month, dayClock.todayFlow, settingsRepository.settings) { m, t, s -> Triple(m, t, s.firstDayOfWeek) },
        completionByDate,
        selectedDate,
        combine(habitsForDate, tasksForDate) { h, t -> h to t },
        weekly
    ) { (m, today, firstDay), completion, selected, (habits, tasks), (thisWeek, lastWeek) ->
        CalendarUiState(m, today, firstDay, thisWeek, lastWeek, completion, selected, habits, tasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    fun selectDate(date: LocalDate) {
        selectedDate.value = if (selectedDate.value == date) null else date
    }

    // Potrdi/preklici opravljenost navade za izbran (tudi pretekli) dan.
    // Prihodnje dni zavrne ToggleHabitUseCase.
    fun toggleHabitOnDate(habit: Habit, date: LocalDate) {
        viewModelScope.launch { toggleHabit(habit, date) }
    }
}
