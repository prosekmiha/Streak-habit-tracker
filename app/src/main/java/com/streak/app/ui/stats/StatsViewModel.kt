package com.streak.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.LogStatus
import com.streak.app.domain.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class StatsUiState(
    val habits: List<HabitWithStreak> = emptyList(),
    val selectedId: String? = null
) {
    val selected: HabitWithStreak? get() = habits.firstOrNull { it.habit.id == selectedId } ?: habits.firstOrNull()

    // Status po datumu za izbrano navado (za heatmap)
    val statusByDate: Map<LocalDate, LogStatus>
        get() = selected?.logs?.associate { it.date to it.status } ?: emptyMap()

    // Delez DONE po zadnjih 5 tednih (0f..1f)
    val weeklyValues: List<Float>
        get() {
            val sel = selected ?: return emptyList()
            val wf = WeekFields.of(Locale.getDefault())
            val today = LocalDate.now()
            return (4 downTo 0).map { weeksAgo ->
                val ref = today.minusWeeks(weeksAgo.toLong())
                val week = ref.get(wf.weekOfWeekBasedYear())
                val year = ref.get(wf.weekBasedYear())
                val done = sel.logs.count {
                    it.status == LogStatus.DONE &&
                        it.date.get(wf.weekOfWeekBasedYear()) == week &&
                        it.date.get(wf.weekBasedYear()) == year
                }
                (done.toFloat() / 7f).coerceIn(0f, 1f)
            }
        }

    // Mesecni odstotek (zadnjih 30 dni)
    val monthRate: Int
        get() {
            val sel = selected ?: return 0
            val today = LocalDate.now()
            val start = today.minusDays(29)
            val done = sel.logs.count { it.status == LogStatus.DONE && !it.date.isBefore(start) && !it.date.isAfter(today) }
            return ((done.toFloat() / 30f) * 100).toInt()
        }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    habitRepository: HabitRepository
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    val state: StateFlow<StatsUiState> = combine(
        habitRepository.observeHabitsWithStreak(),
        selectedId
    ) { habits, sel ->
        StatsUiState(habits = habits, selectedId = sel ?: habits.firstOrNull()?.habit?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun select(id: String) { selectedId.value = id }
}
