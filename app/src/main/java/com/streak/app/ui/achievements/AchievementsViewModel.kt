package com.streak.app.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.gamification.GameState
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class AchievementsUi(val game: GameState, val today: LocalDate)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    gameRepository: GameRepository,
    dayClock: DayClock
) : ViewModel() {
    val state: StateFlow<AchievementsUi?> = combine(gameRepository.state, dayClock.todayFlow) { game, today -> AchievementsUi(game, today) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
