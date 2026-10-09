package com.streak.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay

@Entity(tableName = "habits", indices = [Index("archivedAt")])
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val colorHex: String,
    val category: HabitCategory,
    val goalType: GoalType,
    val targetValue: Double,
    val unit: String?,
    val scheduleType: ScheduleType,
    // Bit 0 = ponedeljek ... bit 6 = nedelja
    val weekdaysMask: Int,
    val timesPerPeriod: Int,
    val timeOfDay: TimeOfDay,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    val archivedAt: Long?,
    val createdAt: Long,
    val sortOrder: Int
)
