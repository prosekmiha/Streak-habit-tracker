package com.streak.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskGroup { OVERDUE, TODAY, UPCOMING, NO_DATE, DONE }

data class TasksUiState(
    val loaded: Boolean = false,
    val groups: List<Pair<TaskGroup, List<Task>>> = emptyList()
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    dayClock: DayClock
) : ViewModel() {

    val state: StateFlow<TasksUiState> = combine(taskRepository.observeTasks(), dayClock.todayFlow) { tasks, today ->
        val grouped = tasks.groupBy { task ->
            val due = task.dueDate
            when {
                task.isDone -> TaskGroup.DONE
                due == null -> TaskGroup.NO_DATE
                due.isBefore(today) -> TaskGroup.OVERDUE
                due == today -> TaskGroup.TODAY
                else -> TaskGroup.UPCOMING
            }
        }
        TasksUiState(
            loaded = true,
            groups = TaskGroup.entries.mapNotNull { g -> grouped[g]?.let { g to it } }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    fun toggle(task: Task) {
        viewModelScope.launch { taskRepository.setDone(task.id, !task.isDone) }
    }
}
