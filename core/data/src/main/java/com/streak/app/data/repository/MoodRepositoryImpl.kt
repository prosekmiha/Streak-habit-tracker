package com.streak.app.data.repository

import com.streak.app.data.local.dao.MoodDao
import com.streak.app.data.mapper.toDomain
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.model.MoodEntry
import com.streak.app.domain.repository.MoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodRepositoryImpl @Inject constructor(
    private val moodDao: MoodDao
) : MoodRepository {

    override fun observe(date: LocalDate): Flow<MoodEntry?> =
        moodDao.observe(date.toEpochDay()).map { it?.toDomain() }

    override fun observeRange(start: LocalDate, end: LocalDate): Flow<List<MoodEntry>> =
        moodDao.observeRange(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<MoodEntry>> =
        moodDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(date: LocalDate): MoodEntry? = moodDao.get(date.toEpochDay())?.toDomain()

    override suspend fun save(entry: MoodEntry) = moodDao.upsert(entry.toEntity())

    override suspend fun delete(date: LocalDate) = moodDao.delete(date.toEpochDay())
}
