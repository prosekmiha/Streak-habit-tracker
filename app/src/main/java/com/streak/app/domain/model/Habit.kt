package com.streak.app.domain.model

import java.time.LocalTime

// Domenski model navade
data class Habit(
    val id: String,
    val title: String,
    val icon: String,
    val colorHex: String,
    val frequency: Frequency,
    val targetPerWeek: Int,
    val reminderMinuteOfDay: Int?, // minuta v dnevu (0..1439) ali null
    val reminderEnabled: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val sortOrder: Int
) {
    val reminderTime: LocalTime?
        get() = reminderMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) }
}
