package com.streak.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
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
import com.streak.app.game.GameRepository

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val habits: List<Habit> = emptyList(),
    val topStreak: Int = 0,
    val totalDone: Int = 0,
    val archived: List<Habit> = emptyList(),
    val recalculating: Boolean = false,
    val level: Int? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val alarmScheduler: AlarmScheduler,
    gameRepository: GameRepository
) : ViewModel() {

    private val recalculating = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        habitRepository.observeHabits(),
        habitRepository.observeArchivedHabits(),
        recalculating,
        gameRepository.state
    ) { settings, habits, archived, recalc, game ->
        SettingsUiState(
            settings = settings,
            habits = habits.map { it.habit },
            topStreak = habits.maxOfOrNull { it.streak.currentStreak } ?: 0,
            totalDone = habits.sumOf { it.streak.totalDone },
            archived = archived,
            recalculating = recalc,
            level = game.level.level
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setName(name: String) = update { it.copy(userName = name.trim().take(40)) }
    fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setAnimations(level: AnimationLevel) = update { it.copy(animationLevel = level) }
    fun setFirstDayOfWeek(day: DayOfWeek) = update { it.copy(firstDayOfWeek = day) }
    fun setPrivacyScreen(enabled: Boolean) = update { it.copy(privacyScreen = enabled) }
    fun setHideWidgetDetails(enabled: Boolean) = update { it.copy(hideWidgetDetails = enabled) }

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
