package com.streak.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.streak.Periods
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val habits: List<HabitWithStreak> = emptyList(),
    // Izbrana navada s celotno zgodovino
    val selected: HabitWithStreak? = null,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
) {
    val today: LocalDate get() = selected?.today ?: LocalDate.now()

    val statusByDate: Map<LocalDate, DayState>
        get() = selected?.days ?: emptyMap()

    // Delez cilja po zadnjih 8 tednih (0f..1f)
    val weeklyValues: List<Float>
        get() {
            val sel = selected ?: return emptyList()
            val perWeek = if (sel.habit.schedule.streakUnit == StreakUnit.WEEK) sel.habit.schedule.timesPerPeriod else 7
            val thisWeek = Periods.start(today, StreakUnit.WEEK, firstDayOfWeek)
            return (7 downTo 0).map { weeksAgo ->
                val start = thisWeek.minusWeeks(weeksAgo.toLong())
                val done = (0L..6L).count { sel.days[start.plusDays(it)] == DayState.DONE }
                (done.toFloat() / perWeek.coerceAtLeast(1)).coerceIn(0f, 1f)
            }
        }

    val monthRate: Int get() = ((selected?.streak?.completionRate ?: 0f) * 100).toInt()

    // Najpogostejsa ura vpisa (mediana lokalnega casa ob vpisu opravljenih dni)
    val typicalTime: java.time.LocalTime?
        get() {
            val minutes = selected?.completions?.values
                ?.filter { it.status == com.streak.app.domain.model.CompletionStatus.DONE && !it.manuallyEdited }
                ?.map { it.loggedAt.atOffset(it.zoneOffset).toLocalTime().let { t -> t.hour * 60 + t.minute } }
                ?.sorted()
                ?.takeIf { it.size >= 3 } ?: return null
            val m = minutes[minutes.size / 2]
            return java.time.LocalTime.of(m / 60, m % 60)
        }

    // Dan v tednu z najvec opravljenimi zapisi
    val bestWeekday: DayOfWeek?
        get() = selected?.days?.filterValues { it == DayState.DONE }?.keys
            ?.groupingBy { it.dayOfWeek }?.eachCount()?.maxByOrNull { it.value }?.key
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    habitRepository: HabitRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)
    private val habits = habitRepository.observeHabits()

    private val selectedDetail = combine(habits, selectedId) { list, sel ->
        sel?.takeIf { id -> list.any { it.habit.id == id } } ?: list.firstOrNull()?.habit?.id
    }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else habitRepository.observeHabit(id) }

    val state: StateFlow<StatsUiState> = combine(
        habits,
        selectedDetail,
        settingsRepository.settings.map { it.firstDayOfWeek }.distinctUntilChanged()
    ) { list, detail, firstDay ->
        StatsUiState(habits = list, selected = detail, firstDayOfWeek = firstDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun select(id: String) { selectedId.value = id }
}
