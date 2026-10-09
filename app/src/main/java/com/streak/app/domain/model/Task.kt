package com.streak.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

// Domenski model naloge
data class Task(
    val id: String,
    val title: String,
    val dueDate: LocalDate?,
    val dueMinuteOfDay: Int?,
    val priority: Priority,
    val isDone: Boolean,
    val isRecurring: Boolean,
    val recurrenceRule: String?,
    val createdAt: Long
) {
    val dueTime: LocalTime?
        get() = dueMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) }
}
