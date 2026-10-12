package com.streak.app.domain.repository

import com.streak.app.domain.gamification.GameState
import kotlinx.coroutines.flow.Flow

// Stanje igrifikacije (XP, nivo, znacke, izziv dneva), izpeljano iz zapisov ob vsaki spremembi
interface GameRepository {
    val state: Flow<GameState>
}
