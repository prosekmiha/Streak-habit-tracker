package com.streak.app.domain.model

import java.time.Instant
import java.time.LocalDate

// Zakljucena casovna seja (meditacija, branje ...)
data class HabitSession(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val startedAt: Instant,
    val endedAt: Instant,
    val durationSeconds: Long
)
