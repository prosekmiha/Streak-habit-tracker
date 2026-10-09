package com.streak.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.streak.app.data.local.dao.HabitDao
import com.streak.app.data.local.dao.HabitLogDao
import com.streak.app.data.local.dao.TaskDao
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitLogEntity
import com.streak.app.data.local.entity.TaskEntity

@Database(
    entities = [HabitEntity::class, HabitLogEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StreakDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "streak.db"
    }
}
