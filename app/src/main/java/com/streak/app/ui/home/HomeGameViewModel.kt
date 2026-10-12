package com.streak.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streak.app.domain.gamification.Challenge
import com.streak.app.domain.gamification.GameCelebration
import com.streak.app.domain.gamification.GameEvents
import com.streak.app.domain.gamification.GameMemory
import com.streak.app.domain.repository.GameMemoryRepository
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.time.DayClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Igrifikacija na domacem zaslonu: praznovanja (znacka, nivo), nabiralnik in nivo za cip
@HiltViewModel
class HomeGameViewModel @Inject constructor(
    private val gameEvents: GameEvents,
    private val memory: GameMemoryRepository,
    private val dayClock: DayClock,
    gameRepository: GameRepository
) : ViewModel() {

    val celebration: StateFlow<GameCelebration?> = gameEvents.celebrations.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val inbox: StateFlow<GameMemory?> = memory.memory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val level: StateFlow<Int?> = gameRepository.state.map { it.level.level }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val challengeDone: Flow<Challenge> = gameEvents.challengeDone

    fun dismissCelebration() = gameEvents.consumeCelebration()

    fun markInboxRead() {
        viewModelScope.launch { memory.markRead(dayClock.now().toEpochMilli()) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
