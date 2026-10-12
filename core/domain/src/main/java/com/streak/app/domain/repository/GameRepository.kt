package com.streak.app.domain.repository

import com.streak.app.domain.gamification.DayActivity
import com.streak.app.domain.gamification.DaySessions
import com.streak.app.domain.gamification.GameState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Stanje igrifikacije (XP, nivo, znacke, izziv dneva), izpeljano iz zapisov ob vsaki spremembi
interface GameRepository {
    val state: Flow<GameState>

    // Dnevna aktivnost in seje za obdobje (pregled meseca/leta)
    suspend fun activityBetween(start: LocalDate, end: LocalDate): List<DayActivity>
    suspend fun sessionsBetween(start: LocalDate, end: LocalDate): List<DaySessions>
}
