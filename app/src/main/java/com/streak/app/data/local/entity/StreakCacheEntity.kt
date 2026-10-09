package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.streak.app.domain.model.StreakUnit

// Predpomnilnik izracunanega streaka; posodobi se ob vsakem vpisu in ob menjavi dneva
@Entity(
    tableName = "streak_cache",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StreakCacheEntity(
    @PrimaryKey val habitId: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate: Float,
    val unit: StreakUnit,
    val totalDone: Int,
    // Logicni dan, za katerega je izracun veljaven
    val computedForEpochDay: Long
)
