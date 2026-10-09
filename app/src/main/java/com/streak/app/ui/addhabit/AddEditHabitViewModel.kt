package com.streak.app.ui.addhabit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.Reminder
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.notifications.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class HabitForm(
    val id: String? = null,
    val title: String = "",
    val icon: String = "DirectionsRun",
    val colorHex: String = "#5dcb80",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val timesPerPeriod: Int = 3,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0
) {
    val isValid: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dayClock: DayClock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String? = savedStateHandle["habitId"]

    // Izvirna navada pri urejanju - polja, ki jih obrazec (se) ne ureja, ostanejo nespremenjena
    private var original: Habit? = null

    private val _form = MutableStateFlow(HabitForm())
    val form: StateFlow<HabitForm> = _form.asStateFlow()

    val isEdit: Boolean get() = habitId != null

    init {
        if (habitId != null) {
            viewModelScope.launch {
                habitRepository.getHabit(habitId)?.let { h ->
                    original = h
                    val reminder = h.reminders.firstOrNull()
                    _form.value = HabitForm(
                        id = h.id, title = h.title, icon = h.icon, colorHex = h.colorHex,
                        scheduleType = h.schedule.type,
                        timesPerPeriod = h.schedule.timesPerPeriod,
                        reminderEnabled = reminder?.enabled ?: false,
                        reminderHour = reminder?.time?.hour ?: 8,
                        reminderMinute = reminder?.time?.minute ?: 0
                    )
                }
            }
        }
    }

    fun update(transform: (HabitForm) -> HabitForm) = _form.update(transform)

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            val reminders = if (f.reminderEnabled) listOf(Reminder(time = LocalTime.of(f.reminderHour, f.reminderMinute))) else emptyList()
            val base = original ?: Habit(
                id = UUID.randomUUID().toString(),
                title = "",
                icon = f.icon,
                colorHex = f.colorHex,
                startDate = dayClock.today(),
                createdAt = dayClock.now().toEpochMilli()
            )
            val habit = base.copy(
                title = f.title.trim(),
                icon = f.icon,
                colorHex = f.colorHex,
                schedule = base.schedule.copy(type = f.scheduleType, timesPerPeriod = f.timesPerPeriod.coerceAtLeast(1)),
                reminders = reminders
            )
            original?.let { alarmScheduler.cancel(it) }
            habitRepository.upsertHabit(habit)
            alarmScheduler.schedule(habit)
            onDone()
        }
    }

    companion object {
        val QuickMonthlyTargets = listOf(1, 2, 4, 8, 12, 20)
    }
}
