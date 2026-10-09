package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("epochDay")]
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationSeconds: Long
)
