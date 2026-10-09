package com.streak.app.domain.model

import java.time.LocalDate

// Navada skupaj z dnevnimi zapisi in izracunanim streakom (za UI)
data class HabitWithStreak(
    val habit: Habit,
    val logs: List<HabitLog>,
    val streak: HabitStreak
) {
    // Stanje navade za podan dan
    fun statusOn(date: LocalDate): LogStatus? =
        logs.firstOrNull { it.date == date }?.status

    val isDoneToday: Boolean
        get() = statusOn(LocalDate.now()) == LogStatus.DONE
}
