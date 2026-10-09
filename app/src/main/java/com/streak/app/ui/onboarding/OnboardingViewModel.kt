package com.streak.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Habit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.time.DayClock
import com.streak.app.notifications.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class OnboardingStep { WELCOME, NAME, NOTIFICATIONS, TEMPLATES }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val name: String = "",
    val selectedTemplates: Set<String> = setOf("reading", "water", "household"),
    val saving: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val alarmScheduler: AlarmScheduler,
    private val dayClock: DayClock
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun goTo(step: OnboardingStep) = _state.update { it.copy(step = step) }

    fun back(): Boolean {
        val prev = OnboardingStep.entries.getOrNull(_state.value.step.ordinal - 1) ?: return false
        goTo(prev)
        return true
    }

    fun setName(name: String) = _state.update { it.copy(name = name.take(MAX_NAME)) }

    fun toggleTemplate(id: String) = _state.update {
        it.copy(selectedTemplates = if (id in it.selectedTemplates) it.selectedTemplates - id else it.selectedTemplates + id)
    }

    // Shrani ime in izbrane predloge ter zakljuci onboarding
    fun finish(withTemplates: Boolean) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val s = _state.value
            if (withTemplates) {
                val today = dayClock.today()
                val now = dayClock.now().toEpochMilli()
                HabitTemplates.all.filter { it.id in s.selectedTemplates }.forEachIndexed { index, t ->
                    val habit = Habit(
                        id = UUID.randomUUID().toString(),
                        title = context.getString(t.title),
                        description = context.getString(t.description),
                        icon = t.icon,
                        colorHex = t.colorHex,
                        category = t.category,
                        goalType = t.goalType,
                        targetValue = t.target,
                        unit = t.unit?.let { context.getString(it) },
                        schedule = t.schedule,
                        timeOfDay = t.timeOfDay,
                        startDate = today,
                        createdAt = now + index,
                        sortOrder = index
                    )
                    habitRepository.upsertHabit(habit)
                    alarmScheduler.schedule(habit)
                }
            }
            val today = dayClock.today()
            settingsRepository.update {
                it.copy(userName = s.name.trim(), onboardingDone = true, lastKnownDate = today)
            }
        }
    }

    private companion object {
        const val MAX_NAME = 40
    }
}
