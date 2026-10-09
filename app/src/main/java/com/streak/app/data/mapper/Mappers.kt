package com.streak.app.data.mapper

import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitLogEntity
import com.streak.app.data.local.entity.TaskEntity
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitLog
import com.streak.app.domain.model.Task
import java.time.LocalDate

// Pretvorbe entitet <-> domenskih modelov

fun HabitEntity.toDomain(): Habit = Habit(
    id, title, icon, colorHex, frequency, targetPerWeek,
    reminderMinuteOfDay, reminderEnabled, isActive, createdAt, sortOrder
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id, title, icon, colorHex, frequency, targetPerWeek,
    reminderMinuteOfDay, reminderEnabled, isActive, createdAt, sortOrder
)

fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    id, habitId, LocalDate.ofEpochDay(epochDay), status, completedAt, note
)

fun TaskEntity.toDomain(): Task = Task(
    id, title,
    dueEpochDay?.let { LocalDate.ofEpochDay(it) },
    dueMinuteOfDay, priority, isDone, isRecurring, recurrenceRule, createdAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id, title, dueDate?.toEpochDay(), dueMinuteOfDay, priority,
    isDone, isRecurring, recurrenceRule, createdAt
)
