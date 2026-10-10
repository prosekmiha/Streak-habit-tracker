package com.streak.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.usecase.ExportDataUseCase
import com.streak.app.notifications.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val habits: List<Habit> = emptyList(),
    val topStreak: Int = 0,
    val totalDone: Int = 0,
    val archived: List<Habit> = emptyList(),
    val exportJson: String? = null,
    val recalculating: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val exportData: ExportDataUseCase,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val exportJson = MutableStateFlow<String?>(null)
    private val recalculating = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        habitRepository.observeHabits(),
        habitRepository.observeArchivedHabits(),
        exportJson,
        recalculating
    ) { settings, habits, archived, export, recalc ->
        SettingsUiState(
            settings = settings,
            habits = habits.map { it.habit },
            topStreak = habits.maxOfOrNull { it.streak.currentStreak } ?: 0,
            totalDone = habits.sumOf { it.streak.totalDone },
            archived = archived,
            exportJson = export,
            recalculating = recalc
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setName(name: String) = update { it.copy(userName = name.trim().take(40)) }
    fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setAnimations(level: AnimationLevel) = update { it.copy(animationLevel = level) }
    fun setFirstDayOfWeek(day: DayOfWeek) = update { it.copy(firstDayOfWeek = day) }

    // Sprememba meje dneva vpliva na "danes", streake in dnevni alarm
    fun setDayEndsAt(time: LocalTime) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(dayEndsAt = time) }
            alarmScheduler.scheduleDailyReset()
            habitRepository.recalculateAll()
        }
    }

    fun unarchive(habit: Habit) {
        viewModelScope.launch {
            habitRepository.setArchived(habit.id, false)
            habitRepository.getHabit(habit.id)?.let { alarmScheduler.schedule(it) }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            alarmScheduler.cancel(habit)
            habitRepository.deleteHabit(habit.id)
        }
    }

    fun export() {
        viewModelScope.launch { exportJson.value = exportData() }
    }

    fun clearExport() { exportJson.value = null }

    fun recalculateAll() {
        if (recalculating.value) return
        viewModelScope.launch {
            recalculating.value = true
            try { habitRepository.recalculateAll() } finally { recalculating.value = false }
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
