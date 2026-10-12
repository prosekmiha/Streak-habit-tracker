package com.streak.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.AnimationLevel
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ThemeMode
import com.streak.app.domain.notify.ReminderScheduler
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.usecase.DeleteHabitUseCase
import com.streak.app.domain.usecase.SetHabitArchivedUseCase
import com.streak.app.tiles.TileUpdater
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
    val recalculating: Boolean = false,
    val level: Int? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val alarmScheduler: ReminderScheduler,
    private val setHabitArchived: SetHabitArchivedUseCase,
    private val deleteHabit: DeleteHabitUseCase,
    private val tileUpdater: TileUpdater,
    private val dayClock: DayClock,
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
    // Tedenski streaki v predpomnilniku so odvisni od prvega dne tedna
    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(firstDayOfWeek = day) }
            habitRepository.recalculateAll()
        }
    }
    fun setPrivacyScreen(enabled: Boolean) = update { it.copy(privacyScreen = enabled) }
    fun setHideWidgetDetails(enabled: Boolean) = update { it.copy(hideWidgetDetails = enabled) }
    fun setTileHabit(id: String?) = update { it.copy(tileHabitId = id) }
    fun setTileSessionHabit(id: String?) = update { it.copy(tileSessionHabitId = id) }

    fun requestAddTile(session: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) runCatching { tileUpdater.requestAdd(session) }
    }

    // Sprememba meje dneva vpliva na "danes", streake in dnevni alarm
    fun setDayEndsAt(time: LocalTime) {
        viewModelScope.launch {
            val before = dayClock.today()
            settingsRepository.update { it.copy(dayEndsAt = time) }
            // Premik meje lahko "danes" premakne za dan nazaj: to ni skok ure
            val after = dayClock.today()
            if (after != before) settingsRepository.update { if (it.lastKnownDate == before) it.copy(lastKnownDate = after) else it }
            alarmScheduler.scheduleDailyReset()
            habitRepository.recalculateAll()
        }
    }

    fun unarchive(habit: Habit) {
        viewModelScope.launch { setHabitArchived(habit.id, archived = false) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { deleteHabit(habit.id) }
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
