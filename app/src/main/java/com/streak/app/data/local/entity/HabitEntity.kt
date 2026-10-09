package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streak.app.domain.model.Frequency

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val icon: String,
    val colorHex: String,
    val frequency: Frequency,
    val targetPerWeek: Int,
    val reminderMinuteOfDay: Int?,
    val reminderEnabled: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val sortOrder: Int
)
