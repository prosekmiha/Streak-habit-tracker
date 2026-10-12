package com.streak.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.SaveTaskUseCase
import com.streak.app.domain.usecase.SetTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TaskGroup { OVERDUE, TODAY, UPCOMING, NO_DATE, DONE }

// Ponavljajoca naloga je bila odkljukana in premaknjena na naslednji termin (za "Razveljavi")
data class TaskAdvanced(val original: Task, val next: LocalDate)

data class TasksUiState(
    val loaded: Boolean = false,
    val groups: List<Pair<TaskGroup, List<Task>>> = emptyList()
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val setTaskDone: SetTaskDoneUseCase,
    private val saveTask: SaveTaskUseCase,
    dayClock: DayClock
) : ViewModel() {

    private val _advanced = Channel<TaskAdvanced>(Channel.BUFFERED)
    val advanced: Flow<TaskAdvanced> = _advanced.receiveAsFlow()

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
        viewModelScope.launch {
            setTaskDone(task, !task.isDone)
            // Ponavljajoca naloga se ne odkljuka, ampak premakne: ponudi razveljavitev
            if (!task.isDone && task.isRecurring && task.dueDate != null) {
                taskRepository.getTask(task.id)?.dueDate?.let { _advanced.send(TaskAdvanced(task, it)) }
            }
        }
    }

    // Vrne nalogo na termin pred odkljukanjem (z opomniki)
    fun undo(original: Task) {
        viewModelScope.launch { saveTask(original) }
    }
}
