package com.streak.app.domain.model

import java.time.LocalDate

// Navada z nedavnimi zapisi in streakom (za UI). `days` vsebuje izpeljana
// stanja za nedavne dni (okno okoli danes), `completions` pa shranjene zapise.
data class HabitWithStreak(
    val habit: Habit,
    val completions: Map<LocalDate, Completion>,
    val days: Map<LocalDate, DayState>,
    val streak: HabitStreak,
    val today: LocalDate
) {
    fun stateOn(date: LocalDate): DayState? = days[date]

    fun completionOn(date: LocalDate): Completion? = completions[date]

    val todayState: DayState? get() = days[today]

    val isDoneToday: Boolean get() = todayState == DayState.DONE

    // Ali je navada danes na sporedu (za stevec "x / y")
    val isDueToday: Boolean
        get() = todayState != null && todayState != DayState.NOT_SCHEDULED && todayState != DayState.INACTIVE

    // Napredek proti cilju za danes (0f..1f)
    val todayProgress: Float
        get() {
            val v = completions[today]?.value ?: 0.0
            return if (habit.targetValue <= 0.0) 0f else (v / habit.targetValue).toFloat().coerceIn(0f, 1f)
        }
}
