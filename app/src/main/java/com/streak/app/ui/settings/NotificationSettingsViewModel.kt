package com.streak.app.ui.settings

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.AppSettings
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.notifications.AlarmScheduler
import com.streak.app.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

// Stanje sistemskih dovoljenj (osvezi se ob vrnitvi iz sistemskih nastavitev)
data class SystemStatus(
    val notificationsEnabled: Boolean = true,
    val exactAlarms: Boolean = true,
    val batteryUnrestricted: Boolean = true
)

data class NotificationSettingsState(
    val settings: AppSettings = AppSettings(),
    val system: SystemStatus = SystemStatus()
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val helper: NotificationHelper,
    private val scheduler: AlarmScheduler
) : ViewModel() {

    private val system = MutableStateFlow(readSystem())

    val state: StateFlow<NotificationSettingsState> = combine(settingsRepository.settings, system) { s, sys ->
        NotificationSettingsState(s, sys)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationSettingsState(system = system.value))

    fun refreshSystem() {
        val before = system.value
        val now = readSystem()
        system.value = now
        // Dovoljenje za natancne alarme je novo: alarmi naj bodo takoj natancni
        if (!before.exactAlarms && now.exactAlarms) viewModelScope.launch { scheduler.rescheduleAll() }
    }

    private fun readSystem() = SystemStatus(
        notificationsEnabled = helper.enabled(),
        exactAlarms = scheduler.canScheduleExact(),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    )

    fun setBriefing(enabled: Boolean) = update { it.copy(briefingEnabled = enabled) }
    fun setBriefingTime(time: LocalTime) = update { it.copy(briefingTime = time) }
    fun setNudge(enabled: Boolean) = update { it.copy(nudgeEnabled = enabled) }
    fun setNudgeTime(time: LocalTime) = update { it.copy(nudgeTime = time) }
    fun setHideNames(hide: Boolean) = update { it.copy(hideNamesInNotifications = hide) }
    fun setCap(cap: Int) = update { it.copy(dailyNotificationCap = cap) }

    // Privzeto 22:00-07:00
    fun setQuietHours(enabled: Boolean) = update {
        if (enabled) it.copy(quietHoursStart = it.quietHoursStart ?: DEFAULT_QUIET_START, quietHoursEnd = it.quietHoursEnd ?: DEFAULT_QUIET_END)
        else it.copy(quietHoursStart = null, quietHoursEnd = null)
    }
    fun setQuietStart(time: LocalTime) = update { it.copy(quietHoursStart = time) }
    fun setQuietEnd(time: LocalTime) = update { it.copy(quietHoursEnd = time) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    companion object {
        val CAP_OPTIONS = listOf(0, 3, 5, 10)
        private val DEFAULT_QUIET_START: LocalTime = LocalTime.of(22, 0)
        private val DEFAULT_QUIET_END: LocalTime = LocalTime.of(7, 0)
    }
}
