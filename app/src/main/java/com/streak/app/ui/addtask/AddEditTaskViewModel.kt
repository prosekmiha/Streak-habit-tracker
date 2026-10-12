package com.streak.app.ui.addtask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskRecurrence
import com.streak.app.domain.model.TaskReminder
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.domain.usecase.DeleteTaskUseCase
import com.streak.app.domain.usecase.SaveTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class TaskForm(
    val id: String? = null,
    val title: String = "",
    val dueDate: LocalDate? = LocalDate.now(),
    val dueHour: Int = 9,
    val dueMinute: Int = 0,
    val hasTime: Boolean = false,
    val priority: Priority = Priority.NORMAL,
    val isRecurring: Boolean = false,
    val recurringDays: Set<DayOfWeek> = emptySet(),
    val reminders: List<TaskReminder> = emptyList(),
    // Predlogi "zjutraj" in "zvecer" iz nastavitev obvestil
    val morningTime: LocalTime = LocalTime.of(8, 0),
    val eveningTime: LocalTime = LocalTime.of(20, 0)
) {
    val isValid: Boolean get() = title.isNotBlank() && !(isRecurring && repeat == TaskRepeat.WEEKLY && recurringDays.isEmpty())
    val repeat: TaskRepeat
        get() = when {
            !isRecurring -> TaskRepeat.NONE
            recurringDays.isEmpty() || recurringDays.size == 7 -> TaskRepeat.DAILY
            else -> TaskRepeat.WEEKLY
        }
}

enum class TaskRepeat { NONE, DAILY, WEEKLY }

@HiltViewModel
class AddEditTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val saveTask: SaveTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle["taskId"]
    val isEdit: Boolean get() = taskId != null

    // Izvirna naloga pri urejanju (ohrani stanje opravljenosti in cas ustvarjanja)
    private var original: Task? = null

    private val _form = MutableStateFlow(TaskForm())
    val form: StateFlow<TaskForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val task = taskId?.let { taskRepository.getTask(it) }
            original = task
            // Nova naloga: obrazec je ze v uporabi, dopolni le predloge ur
            _form.update { it.copy(morningTime = settings.briefingTime, eveningTime = settings.nudgeTime) }
            task?.let { t ->
                _form.value = _form.value.copy(
                    id = t.id, title = t.title, dueDate = t.dueDate,
                    dueHour = (t.dueMinuteOfDay ?: 540) / 60,
                    dueMinute = (t.dueMinuteOfDay ?: 540) % 60,
                    hasTime = t.dueMinuteOfDay != null,
                    priority = t.priority,
                    isRecurring = t.isRecurring,
                    recurringDays = t.recurringDays,
                    reminders = t.reminders
                )
            }
        }
    }

    fun update(transform: (TaskForm) -> TaskForm) = _form.update(transform)

    fun setRepeat(repeat: TaskRepeat) = _form.update {
        when (repeat) {
            TaskRepeat.NONE -> it.copy(isRecurring = false, recurringDays = emptySet())
            TaskRepeat.DAILY -> it.copy(isRecurring = true, recurringDays = emptySet())
            TaskRepeat.WEEKLY -> it.copy(
                isRecurring = true,
                recurringDays = it.recurringDays.takeIf { d -> d.isNotEmpty() && d.size < 7 } ?: setOfNotNull(it.dueDate?.dayOfWeek ?: DayOfWeek.MONDAY)
            )
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _form.value.id ?: return
        viewModelScope.launch {
            deleteTask(id)
            onDone()
        }
    }

    fun addReminder(reminder: TaskReminder) = _form.update {
        if (reminder in it.reminders || it.reminders.size >= TaskReminder.MAX) it
        else it.copy(reminders = it.reminders + reminder)
    }

    fun removeReminder(reminder: TaskReminder) = _form.update { it.copy(reminders = it.reminders - reminder) }

    fun toggleDay(day: DayOfWeek) = _form.update {
        val set = it.recurringDays.toMutableSet()
        if (!set.add(day)) set.remove(day)
        it.copy(recurringDays = set)
    }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            val task = Task(
                id = f.id ?: UUID.randomUUID().toString(),
                title = f.title.trim(),
                dueDate = f.dueDate,
                dueMinuteOfDay = if (f.hasTime) f.dueHour * 60 + f.dueMinute else null,
                priority = f.priority,
                isDone = original?.isDone ?: false,
                isRecurring = f.isRecurring,
                recurrenceRule = if (f.isRecurring) TaskRecurrence.build(f.recurringDays) else null,
                createdAt = original?.createdAt ?: System.currentTimeMillis(),
                reminders = f.reminders
            )
            saveTask(task)
            onDone()
        }
    }
}
