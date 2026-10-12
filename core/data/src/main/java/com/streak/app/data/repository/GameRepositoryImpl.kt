package com.streak.app.data.repository

import com.streak.app.data.local.dao.GameDao
import com.streak.app.data.local.dao.StatsDao
import com.streak.app.data.mapper.toDomain
import com.streak.app.di.ApplicationScope
import com.streak.app.domain.gamification.DayActivity
import com.streak.app.domain.gamification.DaySessions
import com.streak.app.domain.gamification.GameInput
import com.streak.app.domain.gamification.GameState
import com.streak.app.domain.gamification.Gamification
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.repository.GameRepository
import com.streak.app.domain.time.DayClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    gameDao: GameDao,
    statsDao: StatsDao,
    dayClock: DayClock,
    @ApplicationScope scope: CoroutineScope
) : GameRepository {
    override val state: Flow<GameState> = combine(
        gameDao.observeDayActivity(),
        gameDao.observeDaySessions(),
        statsDao.observeAllAggregates(),
        statsDao.observeStreaks(),
        dayClock.todayFlow
    ) { activity, sessions, aggregates, streaks, today ->
        Gamification.compute(
            GameInput(
                activity = activity.map { DayActivity(LocalDate.ofEpochDay(it.epochDay), it.done, it.doneBefore9, it.doneBefore18, it.notes) },
                sessions = sessions.map { DaySessions(LocalDate.ofEpochDay(it.epochDay), it.count, it.seconds, it.units) },
                aggregates = aggregates.map { it.toDomain() },
                bestStreakDays = streaks.maxOfOrNull { it.bestStreak * daysPer(it.unit) } ?: 0,
                today = today
            )
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    private fun daysPer(unit: StreakUnit) = when (unit) {
        StreakUnit.DAY -> 1
        StreakUnit.WEEK -> 7
        StreakUnit.MONTH -> 30
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
