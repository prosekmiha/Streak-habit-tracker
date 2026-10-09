package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.streak.app.domain.model.LogStatus

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index(value = ["habitId", "epochDay"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long, // LocalDate.toEpochDay()
    val status: LogStatus,
    val completedAt: Long?,
    val note: String?
)
