package com.streak.app.domain.time

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Vir "danasnjega" logicnega dne. Lokalni cas naprave je edini vir resnice.
interface DayClock {
    fun now(): Instant
    fun zone(): ZoneId

    // Danasnji logicni dan; pocaka na nalozene nastavitve (dayEndsAt)
    suspend fun today(): LocalDate

    // Logicni dan za podan trenutek
    suspend fun dateOf(instant: Instant): LocalDate

    // Oddaja danasnji logicni dan ob vsaki spremembi (meja dneva, sprememba ure ali pasu)
    val todayFlow: Flow<LocalDate>
}
