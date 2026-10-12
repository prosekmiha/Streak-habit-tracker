package com.streak.app.domain.model

import java.time.LocalDate

// Dopust: v tem obdobju se nobena veriga ne prekine
data class Vacation(
    val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)
}
