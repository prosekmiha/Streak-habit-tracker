package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.streak.app.data.local.entity.DailyAggregateEntity
import com.streak.app.data.local.entity.StreakCacheEntity
import kotlinx.coroutines.flow.Flow

// Predpomnilnik streakov in dnevni povzetki
@Dao
interface StatsDao {

    @Query("SELECT * FROM streak_cache")
    fun observeStreaks(): Flow<List<StreakCacheEntity>>

    @Query("SELECT * FROM streak_cache")
    suspend fun getStreaks(): List<StreakCacheEntity>

    @Upsert
    suspend fun upsertStreak(entity: StreakCacheEntity)

    @Upsert
    suspend fun upsertStreaks(entities: List<StreakCacheEntity>)

    @Query("SELECT * FROM daily_aggregates WHERE epochDay BETWEEN :start AND :end ORDER BY epochDay ASC")
    fun observeAggregates(start: Long, end: Long): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates ORDER BY epochDay ASC")
    fun observeAllAggregates(): Flow<List<DailyAggregateEntity>>

    @Query("SELECT * FROM daily_aggregates ORDER BY epochDay ASC")
    suspend fun getAllAggregates(): List<DailyAggregateEntity>

    @Upsert
    suspend fun upsertAggregates(entities: List<DailyAggregateEntity>)

    @Query("DELETE FROM daily_aggregates WHERE epochDay BETWEEN :start AND :end")
    suspend fun deleteAggregates(start: Long, end: Long)

    @Transaction
    suspend fun replaceAggregates(start: Long, end: Long, entities: List<DailyAggregateEntity>) {
        deleteAggregates(start, end)
        upsertAggregates(entities)
    }
}
