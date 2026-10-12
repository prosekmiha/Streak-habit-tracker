package com.streak.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streak.app.domain.model.Priority

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dueEpochDay: Long?,
    val dueMinuteOfDay: Int?,
    val priority: Priority,
    val isDone: Boolean,
    val isRecurring: Boolean,
    val recurrenceRule: String?,
    val createdAt: Long,
    // TaskReminder.encode; prazno = brez opomnikov
    @ColumnInfo(defaultValue = "''") val reminders: String = ""
)
