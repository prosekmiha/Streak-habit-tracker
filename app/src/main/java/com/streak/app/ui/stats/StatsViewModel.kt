package com.streak.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val habits: List<HabitWithStreak> = emptyList(),
    // Izbrana navada s celotno zgodovino
    val selected: HabitWithStreak? = null
) {
    val today: LocalDate get() = selected?.today ?: LocalDate.now()

    // Stanje po datumu za izbrano navado (za heatmap)
    val statusByDate: Map<LocalDate, DayState>
        get() = selected?.days ?: emptyMap()

    // Delez opravljenih dni po zadnjih 5 tednih (0f..1f)
    val weeklyValues: List<Float>
        get() {
            val days = selected?.days ?: return emptyList()
            return (4 downTo 0).map { weeksAgo ->
                val end = today.minusWeeks(weeksAgo.toLong())
                val done = (0L..6L).count { days[end.minusDays(it)] == DayState.DONE }
                (done / 7f).coerceIn(0f, 1f)
            }
        }

    val monthRate: Int
        get() = ((selected?.streak?.completionRate ?: 0f) * 100).toInt()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    habitRepository: HabitRepository
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)
    private val habits = habitRepository.observeHabits()

    private val selectedDetail = combine(habits, selectedId) { list, sel -> sel ?: list.firstOrNull()?.habit?.id }
        .flatMapLatest { id -> if (id == null) flowOf(null) else habitRepository.observeHabit(id) }

    val state: StateFlow<StatsUiState> = combine(habits, selectedDetail) { list, detail ->
        StatsUiState(habits = list, selected = detail)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun select(id: String) { selectedId.value = id }
}
