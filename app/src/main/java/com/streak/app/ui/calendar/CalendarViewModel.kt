package com.streak.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.LogStatus
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val completionByDate: Map<LocalDate, Float> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val habitsForDate: List<HabitWithStreak> = emptyList(),
    val tasksForDate: List<Task> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val toggleHabit: ToggleHabitUseCase
) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    private val _completionByDate = MutableStateFlow<Map<LocalDate, Float>>(emptyMap())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)

    private val allHabits = habitRepository.observeHabitsWithStreak()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tasksForDate = _selectedDate
        .flatMapLatest { date ->
            if (date != null) taskRepository.observeTasksForDate(date)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<CalendarUiState> = combine(
        _month,
        _completionByDate,
        _selectedDate,
        allHabits,
        tasksForDate
    ) { month, completion, selectedDate, habits, tasks ->
        CalendarUiState(
            month = month,
            completionByDate = completion,
            selectedDate = selectedDate,
            habitsForDate = if (selectedDate != null) habits.filter { it.habit.isActive } else emptyList(),
            tasksForDate = tasks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    init { loadMonth(YearMonth.now()) }

    fun prevMonth() { loadMonth(_month.value.minusMonths(1)) }
    fun nextMonth() { loadMonth(_month.value.plusMonths(1)) }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = if (_selectedDate.value == date) null else date
    }

    // Potrdi/prekliči opravljenost navade za izbran (tudi pretekli) dan
    fun toggleHabitOnDate(habitId: String, date: LocalDate, currentlyDone: Boolean) {
        if (date.isAfter(LocalDate.now())) return
        viewModelScope.launch {
            toggleHabit(habitId, date, currentlyDone)
            loadMonth(_month.value)
        }
    }

    private fun loadMonth(month: YearMonth) {
        _month.value = month
        viewModelScope.launch {
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            val logs = habitRepository.getLogsBetween(start, end)
            val habitCount = habitRepository.getHabitsOnce().size.coerceAtLeast(1)
            val doneByDate = logs.filter { it.status == LogStatus.DONE }
                .groupingBy { it.date }
                .eachCount()
            _completionByDate.value = doneByDate.mapValues { (_, c) ->
                (c.toFloat() / habitCount).coerceIn(0f, 1f)
            }
        }
    }
}
