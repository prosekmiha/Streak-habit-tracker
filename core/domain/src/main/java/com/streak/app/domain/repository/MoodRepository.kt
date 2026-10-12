package com.streak.app.domain.repository

import com.streak.app.domain.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MoodRepository {
    fun observe(date: LocalDate): Flow<MoodEntry?>
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<MoodEntry>>
    fun observeAll(): Flow<List<MoodEntry>>
    suspend fun get(date: LocalDate): MoodEntry?
    suspend fun save(entry: MoodEntry)
    suspend fun delete(date: LocalDate)
}
