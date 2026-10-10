package com.streak.app.ui.addtask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
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
    val recurringDays: Set<DayOfWeek> = emptySet()
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle["taskId"]
    val isEdit: Boolean get() = taskId != null

    // Izvirna naloga pri urejanju (ohrani stanje opravljenosti in cas ustvarjanja)
    private var original: Task? = null

    private val _form = MutableStateFlow(TaskForm())
    val form: StateFlow<TaskForm> = _form.asStateFlow()

    init {
        if (taskId != null) {
            viewModelScope.launch {
                taskRepository.observeTasks().first().firstOrNull { it.id == taskId }?.let { t ->
                    original = t
                    _form.value = TaskForm(
                        id = t.id, title = t.title, dueDate = t.dueDate,
                        dueHour = (t.dueMinuteOfDay ?: 540) / 60,
                        dueMinute = (t.dueMinuteOfDay ?: 540) % 60,
                        hasTime = t.dueMinuteOfDay != null,
                        priority = t.priority,
                        isRecurring = t.isRecurring,
                        recurringDays = parseRule(t.recurrenceRule)
                    )
                }
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
            taskRepository.deleteTask(id)
            onDone()
        }
    }

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
                recurrenceRule = if (f.isRecurring) buildRule(f.recurringDays) else null,
                createdAt = original?.createdAt ?: System.currentTimeMillis()
            )
            taskRepository.upsertTask(task)
            onDone()
        }
    }

    // Zgradi preprost RRULE iz izbranih dni (npr. FREQ=WEEKLY;BYDAY=MO,WE)
    private fun buildRule(days: Set<DayOfWeek>): String {
        if (days.isEmpty()) return "FREQ=DAILY"
        val byday = days.sortedBy { it.value }.joinToString(",") { dayCode(it) }
        return "FREQ=WEEKLY;BYDAY=$byday"
    }

    private fun parseRule(rule: String?): Set<DayOfWeek> {
        if (rule == null) return emptySet()
        val byday = rule.substringAfter("BYDAY=", "").substringBefore(";")
        if (byday.isBlank()) return emptySet()
        return byday.split(",").mapNotNull { codeToDay(it) }.toSet()
    }

    private fun dayCode(d: DayOfWeek) = when (d) {
        DayOfWeek.MONDAY -> "MO"; DayOfWeek.TUESDAY -> "TU"; DayOfWeek.WEDNESDAY -> "WE"
        DayOfWeek.THURSDAY -> "TH"; DayOfWeek.FRIDAY -> "FR"; DayOfWeek.SATURDAY -> "SA"; DayOfWeek.SUNDAY -> "SU"
    }

    private fun codeToDay(c: String) = when (c) {
        "MO" -> DayOfWeek.MONDAY; "TU" -> DayOfWeek.TUESDAY; "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY; "FR" -> DayOfWeek.FRIDAY; "SA" -> DayOfWeek.SATURDAY; "SU" -> DayOfWeek.SUNDAY
        else -> null
    }
}
