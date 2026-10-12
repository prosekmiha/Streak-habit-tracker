package com.streak.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Urnik navade
data class HabitSchedule(
    val type: ScheduleType = ScheduleType.DAILY,
    // Uporablja se pri WEEKDAYS
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    // Uporablja se pri TIMES_PER_WEEK / TIMES_PER_MONTH
    val timesPerPeriod: Int = 1
) {
    val streakUnit: StreakUnit
        get() = when (type) {
            ScheduleType.DAILY, ScheduleType.WEEKDAYS -> StreakUnit.DAY
            ScheduleType.TIMES_PER_WEEK -> StreakUnit.WEEK
            ScheduleType.TIMES_PER_MONTH -> StreakUnit.MONTH
        }

    companion object {
        val Daily = HabitSchedule()
    }
}

data class Reminder(
    val id: Long = 0,
    val time: LocalTime,
    val enabled: Boolean = true
)

// Domenski model navade
data class Habit(
    val id: String,
    val title: String,
    val description: String = "",
    val icon: String,
    val colorHex: String,
    val category: HabitCategory = HabitCategory.NONE,
    val goalType: GoalType = GoalType.YES_NO,
    // YES_NO: 1, QUANTITY: kolicina, DURATION: sekunde
    val targetValue: Double = 1.0,
    val unit: String? = null,
    val schedule: HabitSchedule = HabitSchedule.Daily,
    val timeOfDay: TimeOfDay = TimeOfDay.ANYTIME,
    val reminders: List<Reminder> = emptyList(),
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val sortOrder: Int = 0
) {
    val isArchived: Boolean get() = archivedAt != null

    // Ali je navada na ta dan aktivna (med zacetkom in koncem)
    fun isActiveOn(date: LocalDate): Boolean =
        !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))

    // Ali vrednost doseze cilj
    fun meetsGoal(value: Double): Boolean = when (goalType) {
        GoalType.YES_NO -> value > 0.0
        GoalType.QUANTITY, GoalType.DURATION -> value >= targetValue
        GoalType.QUIT -> value <= 0.0
    }

    val isQuit: Boolean get() = goalType == GoalType.QUIT

    // Ciljno trajanje casovne navade v sekundah (0 pri ostalih)
    fun targetSeconds(): Long = if (goalType == GoalType.DURATION) targetValue.toLong().coerceAtLeast(0) else 0
}
