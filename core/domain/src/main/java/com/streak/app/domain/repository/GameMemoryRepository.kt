package com.streak.app.domain.repository

import com.streak.app.domain.gamification.GameMemory
import kotlinx.coroutines.flow.Flow

// Kaj je uporabnik ze videl (znacke, nivo, izziv) in nabiralnik. XP in znacke same so izpeljane
// iz podatkov; ta shramba le prepreci ponovna praznovanja.
interface GameMemoryRepository {
    val memory: Flow<GameMemory>

    suspend fun current(): GameMemory
    suspend fun update(transform: (GameMemory) -> GameMemory)
    suspend fun markRead(now: Long) = update { it.copy(readUntil = now) }

    // Po uvozu ali brisanju podatkov: naslednji izracun stanje le zapomni (brez plaza praznovanj)
    suspend fun resync() = update { it.copy(initialized = false) }

    suspend fun clear()
}
