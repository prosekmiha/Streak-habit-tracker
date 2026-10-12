package com.streak.app.domain.gamification

import com.streak.app.domain.repository.GameMemoryRepository
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.time.DayClock
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

// Kaj praznujemo na domacem zaslonu
sealed interface GameCelebration {
    data class NewBadge(val badge: Badge) : GameCelebration
    data class LevelUp(val level: Int) : GameCelebration
}

// Zazna nove znacke, nov nivo in opravljen izziv dneva: zapise jih v nabiralnik in
// jih (dokler proces tece) ponudi domacemu zaslonu za praznovanje.
@Singleton
class GameEvents @Inject constructor(
    private val repository: GameRepository,
    private val store: GameMemoryRepository,
    private val dayClock: DayClock
) {
    private val _celebrations = MutableStateFlow<List<GameCelebration>>(emptyList())
    val celebrations: StateFlow<List<GameCelebration>> = _celebrations.asStateFlow()

    private val _challengeDone = MutableSharedFlow<Challenge>(extraBufferCapacity = 1)
    val challengeDone: SharedFlow<Challenge> = _challengeDone.asSharedFlow()

    private val mutex = Mutex()
    private var quiet = false

    suspend fun run() {
        repository.state.collect { state -> mutex.withLock { if (!quiet) onState(state) } }
    }

    // Uvoz, obnova ali brisanje: med spremembo ni praznovanj, potem se novo stanje le zapomni
    @OptIn(FlowPreview::class)
    suspend fun <T> quietly(block: suspend () -> T): T {
        mutex.withLock { quiet = true }
        try {
            return block()
        } finally {
            store.resync()
            val settled = repository.state.debounce(SETTLE_MS).first()
            mutex.withLock {
                onState(settled)
                quiet = false
            }
        }
    }

    fun consumeCelebration() = _celebrations.update { it.drop(1) }

    private suspend fun onState(state: GameState) {
        val today = dayClock.today().toEpochDay()
        val memory = store.current()
        val earned = state.earned.map { it.name }.toSet()
        if (!memory.initialized) {
            store.update {
                it.copy(
                    initialized = true,
                    seenBadges = it.seenBadges + earned,
                    lastLevel = maxOf(it.lastLevel, state.level.level),
                    lastChallengeDay = if (state.today.done) today else it.lastChallengeDay
                )
            }
            return
        }
        val newBadges = Badge.entries.filter { it.name in earned && it.name !in memory.seenBadges }
        val levelUp = state.level.level > memory.lastLevel
        val challengeNow = state.today.done && memory.lastChallengeDay != today
        if (newBadges.isEmpty() && !levelUp && !challengeNow) return

        val now = dayClock.now().toEpochMilli()
        val events = buildList {
            newBadges.forEach { add(InboxEvent(InboxType.BADGE, it.name, now)) }
            if (levelUp) add(InboxEvent(InboxType.LEVEL, state.level.level.toString(), now))
            if (challengeNow) add(InboxEvent(InboxType.CHALLENGE, state.today.challenge.name, now))
        }
        store.update {
            it.copy(
                seenBadges = it.seenBadges + newBadges.map { b -> b.name },
                lastLevel = maxOf(it.lastLevel, state.level.level),
                lastChallengeDay = if (challengeNow) today else it.lastChallengeDay,
                events = it.events + events
            )
        }
        _celebrations.update { queue ->
            // Mejnike verig ze praznuje domaci zaslon, zato so te znacke le v nabiralniku
            queue + newBadges.filter { it.kind != BadgeKind.STREAK }.map { GameCelebration.NewBadge(it) } + listOfNotNull(GameCelebration.LevelUp(state.level.level).takeIf { levelUp })
        }
        if (challengeNow) _challengeDone.tryEmit(state.today.challenge)
    }

    private companion object {
        const val SETTLE_MS = 500L
    }
}
