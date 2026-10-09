package com.streak.app.ui.addhabit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Frequency
import com.streak.app.domain.model.Habit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.notifications.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HabitForm(
    val id: String? = null,
    val title: String = "",
    val icon: String = "DirectionsRun",
    val colorHex: String = "#5dcb80",
    val frequency: Frequency = Frequency.DAILY,
    val targetPerWeek: Int = 7,
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val habitId: String? = savedStateHandle["habitId"]

    private val _form = MutableStateFlow(HabitForm())
    val form: StateFlow<HabitForm> = _form.asStateFlow()

    val isEdit: Boolean get() = habitId != null

    init {
        if (habitId != null) {
            viewModelScope.launch {
                habitRepository.observeHabit(habitId).first()?.let { hws ->
                    val h = hws.habit
                    _form.value = HabitForm(
                        id = h.id, title = h.title, icon = h.icon, colorHex = h.colorHex,
                        frequency = h.frequency, targetPerWeek = h.targetPerWeek,
                        reminderEnabled = h.reminderEnabled,
                        reminderHour = (h.reminderMinuteOfDay ?: 480) / 60,
                        reminderMinute = (h.reminderMinuteOfDay ?: 480) % 60
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
            val reminderMin = if (f.reminderEnabled) f.reminderHour * 60 + f.reminderMinute else null
            val habit = Habit(
                id = f.id ?: UUID.randomUUID().toString(),
                title = f.title.trim(),
                icon = f.icon,
                colorHex = f.colorHex,
                frequency = f.frequency,
                targetPerWeek = f.targetPerWeek.coerceIn(1, 7),
                reminderMinuteOfDay = reminderMin,
                reminderEnabled = f.reminderEnabled,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                sortOrder = 0
            )
            habitRepository.upsertHabit(habit)
            // Razporedi ali preklici alarm glede na opomnik
            alarmScheduler.cancel(habit.id)
            if (habit.reminderEnabled) alarmScheduler.schedule(habit)
            onDone()
        }
    }
}
