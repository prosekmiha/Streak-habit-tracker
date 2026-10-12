package com.streak.app.domain.model

import java.time.DayOfWeek
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
    val createdAt: Long,
    val reminders: List<TaskReminder> = emptyList()
) {
    val dueTime: LocalTime?
        get() = dueMinuteOfDay?.let { LocalTime.of(it / 60, it % 60) }

    // Dnevi ponavljanja; prazno = vsak dan
    val recurringDays: Set<DayOfWeek>
        get() = TaskRecurrence.parseDays(recurrenceRule)

    // Ali ima naloga na ta dan termin (enkratna: rok, ponavljajoca: od roka naprej po pravilu)
    fun occursOn(date: LocalDate): Boolean {
        val due = dueDate ?: return false
        if (!isRecurring) return date == due
        if (date.isBefore(due)) return false
        val days = recurringDays
        return days.isEmpty() || date.dayOfWeek in days
    }

    // Prvi termin na dan "from" ali kasneje; null, ce ga ni (brez roka, enkratna pred "from")
    fun nextOccurrence(from: LocalDate): LocalDate? {
        val due = dueDate ?: return null
        if (!isRecurring) return due.takeIf { !it.isBefore(from) }
        val start = maxOf(due, from)
        // Tedensko pravilo se ponovi v najvec 7 dneh
        return (0L until 7L).map { start.plusDays(it) }.firstOrNull { occursOn(it) }
    }
}

// Preprost RRULE: FREQ=DAILY ali FREQ=WEEKLY;BYDAY=MO,WE
object TaskRecurrence {
    fun build(days: Set<DayOfWeek>): String {
        if (days.isEmpty() || days.size == 7) return "FREQ=DAILY"
        return "FREQ=WEEKLY;BYDAY=" + days.sortedBy { it.value }.joinToString(",") { CODES.getValue(it) }
    }

    fun parseDays(rule: String?): Set<DayOfWeek> {
        val byday = rule?.substringAfter("BYDAY=", "")?.substringBefore(";").orEmpty()
        if (byday.isBlank()) return emptySet()
        return byday.split(",").mapNotNull { code -> CODES.entries.firstOrNull { it.value == code }?.key }.toSet()
    }

    private val CODES = mapOf(
        DayOfWeek.MONDAY to "MO", DayOfWeek.TUESDAY to "TU", DayOfWeek.WEDNESDAY to "WE", DayOfWeek.THURSDAY to "TH",
        DayOfWeek.FRIDAY to "FR", DayOfWeek.SATURDAY to "SA", DayOfWeek.SUNDAY to "SU"
    )
}
