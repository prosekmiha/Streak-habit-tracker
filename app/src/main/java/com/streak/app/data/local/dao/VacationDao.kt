package com.streak.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.streak.app.data.local.entity.VacationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VacationDao {
    @Query("SELECT * FROM vacations ORDER BY startEpochDay ASC")
    fun observeAll(): Flow<List<VacationEntity>>

    @Query("SELECT * FROM vacations ORDER BY startEpochDay ASC")
    suspend fun getAll(): List<VacationEntity>

    @Insert
    suspend fun insert(vacation: VacationEntity): Long

    @Query("DELETE FROM vacations WHERE id = :id")
    suspend fun delete(id: Long)
}
