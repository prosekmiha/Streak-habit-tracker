package com.streak.app.domain.model

import java.time.LocalDate

// Domenski model dnevnega zapisa navade
data class HabitLog(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val status: LogStatus,
    val completedAt: Long?,
    val note: String?
)
