package com.streak.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.plan.WeekPlan
import com.streak.app.domain.plan.WeekPlanner
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.streak.Periods
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class WeekPlanUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val weekStart: LocalDate = LocalDate.now(),
    val isCurrentWeek: Boolean = true,
    val plan: WeekPlan = WeekPlan(emptyList(), emptyList(), emptyList())
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeekPlanViewModel @Inject constructor(
    habitRepository: HabitRepository,
    settingsRepository: SettingsRepository,
    dayClock: DayClock
) : ViewModel() {

    // Zamik od tekocega tedna (0 = ta teden)
    private val weekOffset = MutableStateFlow(0L)

    val state: StateFlow<WeekPlanUiState> = combine(
        dayClock.todayFlow,
        settingsRepository.settings.map { it.firstDayOfWeek }.distinctUntilChanged(),
        weekOffset
    ) { today, firstDay, offset -> Triple(today, Periods.start(today, StreakUnit.WEEK, firstDay).plusWeeks(offset), offset) }
        .flatMapLatest { (today, weekStart, offset) ->
            val weekEnd = weekStart.plusDays(6)
            // Zapisi za cel teden in mesece, ki jih teden zajema (za "X-krat na mesec")
            val from = minOf(weekStart, weekStart.withDayOfMonth(1), today.withDayOfMonth(1))
            val to = maxOf(weekEnd, weekEnd.withDayOfMonth(weekEnd.lengthOfMonth()))
            combine(
                habitRepository.observeHabits(),
                habitRepository.observeCompletionsBetween(from, to),
                habitRepository.observeVacations()
            ) { habits, completions, vacations ->
                WeekPlanUiState(
                    loaded = true,
                    today = today,
                    weekStart = weekStart,
                    isCurrentWeek = offset == 0L,
                    plan = WeekPlanner.build(habits.map { it.habit }, weekStart, completions, vacations, today)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekPlanUiState())

    fun prevWeek() { weekOffset.value -= 1 }
    fun nextWeek() { weekOffset.value += 1 }
    fun thisWeek() { weekOffset.value = 0 }
}
