package com.streak.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val cloudSync: Boolean = false,
    val habits: List<Habit> = emptyList(),
    val exportJson: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val exportData: ExportDataUseCase,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val exportJson = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.userName,
        settingsRepository.themeMode,
        settingsRepository.cloudSyncEnabled,
        habitRepository.observeHabitsWithStreak().map { list -> list.map { it.habit } },
        exportJson
    ) { name, theme, cloud, habits, export ->
        SettingsUiState(name, theme, cloud, habits, export)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setName(name: String) { viewModelScope.launch { settingsRepository.setUserName(name) } }
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { settingsRepository.setThemeMode(mode) } }
    fun setCloud(enabled: Boolean) { viewModelScope.launch { settingsRepository.setCloudSync(enabled) } }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            alarmScheduler.cancel(habit.id)
            habitRepository.deleteHabit(habit.id)
        }
    }

    fun export() {
        viewModelScope.launch { exportJson.value = exportData() }
    }

    fun clearExport() { exportJson.value = null }
}
