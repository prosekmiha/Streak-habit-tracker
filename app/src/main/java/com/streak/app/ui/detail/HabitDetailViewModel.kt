package com.streak.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.domain.repository.SettingsRepository
import com.streak.app.domain.streak.Periods
import com.streak.app.domain.usecase.LogResult
import com.streak.app.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class DetailDay(val date: LocalDate, val state: DayState)

data class HabitDetailState(
    val loaded: Boolean = false,
    val item: HabitWithStreak? = null,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    // 5 tednov (35 dni), zadnji teden je tekoci
    val grid: List<DetailDay> = emptyList(),
    val notes: List<Completion> = emptyList(),
    // Zgodovina casovnih sej (zadnjih 10) in skupni povzetek
    val sessions: List<HabitSession> = emptyList(),
    val sessionCount: Int = 0,
    val sessionTotalSeconds: Long = 0
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    settingsRepository: SettingsRepository,
    private val logHabit: ToggleHabitUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val habitId: String = checkNotNull(savedStateHandle["habitId"])
    private val events = Channel<LogResult>(Channel.BUFFERED)
    val blocked = events.receiveAsFlow()

    val state: StateFlow<HabitDetailState> = combine(
        habitRepository.observeHabit(habitId),
        settingsRepository.settings,
        habitRepository.observeSessions(habitId)
    ) { item, settings, sessions ->
        if (item == null) return@combine HabitDetailState(loaded = true)
        val firstDay = settings.firstDayOfWeek
        val weekStart = Periods.start(item.today, StreakUnit.WEEK, firstDay)
        val start = weekStart.minusWeeks(4)
        val grid = (0L until 35L).map { offset ->
            val d = start.plusDays(offset)
            DetailDay(d, item.stateOn(d) ?: if (d.isAfter(item.today)) DayState.FUTURE else DayState.INACTIVE)
        }
        HabitDetailState(
            loaded = true,
            item = item,
            firstDayOfWeek = firstDay,
            grid = grid,
            notes = item.completions.values.filter { !it.note.isNullOrBlank() }.sortedByDescending { it.date }.take(20),
            sessions = sessions.sortedByDescending { it.startedAt }.take(10),
            sessionCount = sessions.size,
            sessionTotalSeconds = sessions.sumOf { it.durationSeconds }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitDetailState())

    // Naknadni vpis: dotik dneva preklopi opravljenost
    fun toggleDay(date: LocalDate) {
        val habit = state.value.item?.habit ?: return
        viewModelScope.launch {
            val result = logHabit(habit, date)
            if (result == LogResult.FUTURE_BLOCKED) events.send(result)
        }
    }
}
