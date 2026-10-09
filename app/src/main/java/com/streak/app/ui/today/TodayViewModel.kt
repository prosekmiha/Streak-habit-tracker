package com.streak.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val userName: String = "",
    val habits: List<HabitWithStreak> = emptyList(),
    val tasks: List<Task> = emptyList()
) {
    val doneCount: Int get() = habits.count { it.isDoneToday }
    val totalCount: Int get() = habits.count { it.isDueToday || it.isDoneToday }
    val progress: Float get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
    val bestStreak: Int get() = habits.maxOfOrNull { it.streak.currentStreak } ?: 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    settingsRepository: SettingsRepository,
    dayClock: DayClock,
    private val toggleHabit: ToggleHabitUseCase
) : ViewModel() {

    val state: StateFlow<TodayUiState> = combine(
        habitRepository.observeHabits(),
        dayClock.todayFlow.flatMapLatest { taskRepository.observeTasksForDate(it) },
        settingsRepository.userName
    ) { habits, tasks, name ->
        TodayUiState(userName = name, habits = habits, tasks = tasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    fun toggle(item: HabitWithStreak) {
        viewModelScope.launch { toggleHabit(item.habit, item.today) }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch { taskRepository.setDone(task.id, !task.isDone) }
    }
}
