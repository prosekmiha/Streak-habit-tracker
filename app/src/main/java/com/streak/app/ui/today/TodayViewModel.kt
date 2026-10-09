package com.streak.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val userName: String = "",
    val habits: List<HabitWithStreak> = emptyList(),
    val tasks: List<Task> = emptyList()
) {
    val doneCount: Int get() = habits.count { it.isDoneToday }
    val totalCount: Int get() = habits.size
    val progress: Float get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
    val bestStreak: Int get() = habits.maxOfOrNull { it.streak.currentStreak } ?: 0
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val toggleHabit: ToggleHabitUseCase
) : ViewModel() {

    val state: StateFlow<TodayUiState> = combine(
        habitRepository.observeHabitsWithStreak(),
        taskRepository.observeTasksForDate(LocalDate.now()),
        settingsRepository.userName
    ) { habits, tasks, name ->
        TodayUiState(userName = name, habits = habits, tasks = tasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun toggle(item: HabitWithStreak) {
        viewModelScope.launch {
            toggleHabit(item.habit.id, LocalDate.now(), item.isDoneToday)
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { taskRepository.setDone(task.id, !task.isDone) }
    }
}
