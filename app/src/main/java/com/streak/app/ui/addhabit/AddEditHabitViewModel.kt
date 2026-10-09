package com.streak.app.ui.addhabit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.HabitSchedule
import com.streak.app.domain.model.Reminder
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.notifications.AlarmScheduler
import com.streak.app.ui.components.IconMap
import com.streak.app.ui.components.formatAmount
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

data class HabitForm(
    val loaded: Boolean = false,
    val title: String = "",
    val description: String = "",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val timesPerPeriod: Int = 3,
    val goalType: GoalType = GoalType.YES_NO,
    val targetText: String = "",
    val unit: String = "",
    val durationMinutes: Int = 10,
    val icon: String = IconMap.all.first().name,
    val colorHex: String = IconMap.all.first().tileHex,
    // Ce uporabnik barve ni izbral sam, sledi barvi izbrane ikone
    val colorTouched: Boolean = false,
    val category: HabitCategory = HabitCategory.NONE,
    val timeOfDay: TimeOfDay = TimeOfDay.ANYTIME,
    val reminders: List<LocalTime> = emptyList(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val isArchived: Boolean = false
) {
    val target: Double? get() = targetText.replace(',', '.').toDoubleOrNull()

    val titleError: Boolean get() = title.isBlank()
    val targetError: Boolean get() = goalType == GoalType.QUANTITY && (target ?: 0.0) <= 0.0
    val weekdaysError: Boolean get() = scheduleType == ScheduleType.WEEKDAYS && weekdays.isEmpty()
    val endDateError: Boolean get() = endDate != null && endDate.isBefore(startDate)
    val isValid: Boolean get() = !titleError && !targetError && !weekdaysError && !endDateError

    // Dnevi tedna v vrstnem redu glede na nastavitev prvega dne
    val orderedWeekdays: List<DayOfWeek> get() = (0L..6L).map { firstDayOfWeek.plus(it) }
}

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dayClock: DayClock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String? = savedStateHandle["habitId"]

    // Izvirna navada pri urejanju - polja, ki jih obrazec ne ureja, ostanejo nespremenjena
    private var original: Habit? = null

    private val _form = MutableStateFlow(HabitForm())
    val form: StateFlow<HabitForm> = _form.asStateFlow()

    // Ali je bil poskus shranjevanja (napake prikazemo sele po njem)
    private val _showErrors = MutableStateFlow(false)
    val showErrors: StateFlow<Boolean> = _showErrors.asStateFlow()

    val isEdit: Boolean get() = habitId != null

    init {
        viewModelScope.launch {
            val firstDay = settingsRepository.settings.first().firstDayOfWeek
            val today = dayClock.today()
            val habit = habitId?.let { habitRepository.getHabit(it) }
            original = habit
            _form.value = habit?.toForm(firstDay) ?: HabitForm(loaded = true, startDate = today, firstDayOfWeek = firstDay)
        }
    }

    fun update(transform: (HabitForm) -> HabitForm) = _form.update(transform)

    fun selectIcon(name: String) = _form.update {
        val tile = IconMap.find(name).tileHex
        it.copy(icon = name, colorHex = if (it.colorTouched) it.colorHex else tile)
    }

    fun selectColor(hex: String) = _form.update { it.copy(colorHex = hex, colorTouched = true) }

    fun toggleWeekday(day: DayOfWeek) = _form.update {
        it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day)
    }

    fun addReminder(time: LocalTime) = _form.update {
        if (time in it.reminders || it.reminders.size >= AlarmScheduler.MAX_REMINDERS) it
        else it.copy(reminders = (it.reminders + time).sorted())
    }

    fun removeReminder(time: LocalTime) = _form.update { it.copy(reminders = it.reminders - time) }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (!f.isValid) {
            _showErrors.value = true
            return
        }
        viewModelScope.launch {
            val base = original ?: Habit(
                id = UUID.randomUUID().toString(),
                title = "",
                icon = f.icon,
                colorHex = f.colorHex,
                startDate = f.startDate,
                createdAt = dayClock.now().toEpochMilli(),
                sortOrder = habitRepository.getHabits().size
            )
            val target = when (f.goalType) {
                GoalType.YES_NO -> 1.0
                GoalType.QUANTITY -> f.target ?: 1.0
                GoalType.DURATION -> f.durationMinutes.coerceAtLeast(1) * 60.0
            }
            val habit = base.copy(
                title = f.title.trim(),
                description = f.description.trim(),
                icon = f.icon,
                colorHex = f.colorHex,
                category = f.category,
                goalType = f.goalType,
                targetValue = target,
                unit = f.unit.trim().ifBlank { null }.takeIf { f.goalType == GoalType.QUANTITY },
                schedule = HabitSchedule(f.scheduleType, f.weekdays, f.timesPerPeriod.coerceAtLeast(1)),
                timeOfDay = f.timeOfDay,
                reminders = f.reminders.map { Reminder(time = it) },
                startDate = f.startDate,
                endDate = f.endDate
            )
            original?.let { alarmScheduler.cancel(it) }
            habitRepository.upsertHabit(habit)
            alarmScheduler.schedule(habit)
            onDone()
        }
    }

    fun setArchived(archived: Boolean, onDone: () -> Unit) {
        val habit = original ?: return
        viewModelScope.launch {
            if (archived) alarmScheduler.cancel(habit)
            habitRepository.setArchived(habit.id, archived)
            if (!archived) habitRepository.getHabit(habit.id)?.let { alarmScheduler.schedule(it) }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val habit = original ?: return
        viewModelScope.launch {
            alarmScheduler.cancel(habit)
            habitRepository.deleteHabit(habit.id)
            onDone()
        }
    }

    private fun Habit.toForm(firstDay: DayOfWeek) = HabitForm(
        loaded = true,
        title = title,
        description = description,
        scheduleType = schedule.type,
        weekdays = schedule.weekdays,
        timesPerPeriod = schedule.timesPerPeriod,
        goalType = goalType,
        targetText = if (goalType == GoalType.QUANTITY) formatAmount(targetValue) else "",
        unit = unit.orEmpty(),
        durationMinutes = if (goalType == GoalType.DURATION) (targetValue / 60).toInt() else 10,
        icon = icon,
        colorHex = colorHex,
        colorTouched = true,
        category = category,
        timeOfDay = timeOfDay,
        reminders = reminders.map { it.time },
        startDate = startDate,
        endDate = endDate,
        firstDayOfWeek = firstDay,
        isArchived = isArchived
    )
}
