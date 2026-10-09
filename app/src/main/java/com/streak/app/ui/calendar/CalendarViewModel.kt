package com.streak.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.TaskRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

// Navada in njeno stanje za izbran dan
data class HabitDay(val habit: Habit, val state: DayState)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
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

    val state: StateFlow<CalendarUiState> = combine(
        combine(month, dayClock.todayFlow) { m, t -> m to t },
        completionByDate,
        selectedDate,
        habitsForDate,
        tasksForDate
    ) { (m, today), completion, selected, habits, tasks ->
        CalendarUiState(m, today, completion, selected, habits, tasks)
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
