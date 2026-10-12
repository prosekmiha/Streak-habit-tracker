package com.streak.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.mood.MoodInsights
import com.streak.app.domain.mood.MoodStats
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.MoodRepository
import com.streak.app.domain.repository.SettingsRepository
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
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MoodStatsUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    // Razpolozenje po dnevih izbranega meseca
    val moodByDate: Map<LocalDate, Int> = emptyMap(),
    // Povzetek zadnjih WINDOW_DAYS dni
    val stats: MoodStats = MoodStats.Empty,
    val topHabitTitle: String? = null,
    val loaded: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoodStatsViewModel @Inject constructor(
    moodRepository: MoodRepository,
    habitRepository: HabitRepository,
    settingsRepository: SettingsRepository,
    dayClock: DayClock
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    private val monthMoods = month.flatMapLatest { m ->
        moodRepository.observeRange(m.atDay(1), m.atEndOfMonth()).map { list -> list.associate { it.date to it.mood } }
    }

    private val insights = dayClock.todayFlow.flatMapLatest { today ->
        val start = today.minusDays(WINDOW_DAYS - 1)
        combine(
            moodRepository.observeRange(start, today),
            habitRepository.observeAggregates(start, today),
            habitRepository.observeCompletionsBetween(start, today)
        ) { moods, aggregates, completions -> MoodInsights.compute(moods, aggregates, completions) }
    }

    val state: StateFlow<MoodStatsUiState> = combine(
        month,
        monthMoods,
        insights,
        habitRepository.observeHabits(),
        combine(dayClock.todayFlow, settingsRepository.settings.map { it.firstDayOfWeek }.distinctUntilChanged(), ::Pair)
    ) { m, moods, stats, habits, (today, firstDay) ->
        MoodStatsUiState(
            month = m,
            today = today,
            firstDayOfWeek = firstDay,
            moodByDate = moods,
            stats = stats,
            topHabitTitle = stats.topHabitId?.let { id -> habits.firstOrNull { it.habit.id == id }?.habit?.title },
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoodStatsUiState())

    fun prevMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    private companion object {
        const val WINDOW_DAYS = 90L
    }
}
