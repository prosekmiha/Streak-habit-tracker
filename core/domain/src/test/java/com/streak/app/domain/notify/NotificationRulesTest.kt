package com.streak.app.domain.notify

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationRulesTest {

    private val ljubljana = ZoneId.of("Europe/Ljubljana")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), ljubljana)

    @Test
    fun `next occurrence later today`() {
        val next = NotificationRules.nextOccurrence(at("2026-05-10T07:00"), LocalTime.of(8, 30))
        assertEquals(at("2026-05-10T08:30"), next)
    }

    @Test
    fun `next occurrence tomorrow when time passed or exactly now`() {
        assertEquals(at("2026-05-11T08:30"), NotificationRules.nextOccurrence(at("2026-05-10T09:00"), LocalTime.of(8, 30)))
        assertEquals(at("2026-05-11T08:30"), NotificationRules.nextOccurrence(at("2026-05-10T08:30"), LocalTime.of(8, 30)))
    }

    @Test
    fun `reminder in DST gap moves forward instead of disappearing`() {
        // 29. 3. 2026 ob 02:00 se ura premakne na 03:00 - 02:30 ne obstaja
        val next = NotificationRules.nextOccurrence(at("2026-03-29T01:00"), LocalTime.of(2, 30))
        assertEquals(LocalDate.of(2026, 3, 29), next.toLocalDate())
        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
    }

    @Test
    fun `reminder in DST overlap fires once at first occurrence`() {
        // 25. 10. 2026 se ura 02:30 ponovi; izberemo prvo (poletni cas, +02:00)
        val next = NotificationRules.nextOccurrence(at("2026-10-25T01:00"), LocalTime.of(2, 30))
        assertEquals(2, next.offset.totalSeconds / 3600)
    }

    @Test
    fun `quiet hours within a day`() {
        val s = LocalTime.of(13, 0)
        val e = LocalTime.of(15, 0)
        assertTrue(NotificationRules.inQuietHours(s, e, LocalTime.of(13, 0)))
        assertTrue(NotificationRules.inQuietHours(s, e, LocalTime.of(14, 59)))
        assertFalse(NotificationRules.inQuietHours(s, e, LocalTime.of(15, 0)))
        assertFalse(NotificationRules.inQuietHours(s, e, LocalTime.of(12, 59)))
    }

    @Test
    fun `quiet hours across midnight`() {
        val s = LocalTime.of(22, 0)
        val e = LocalTime.of(7, 0)
        assertTrue(NotificationRules.inQuietHours(s, e, LocalTime.of(23, 30)))
        assertTrue(NotificationRules.inQuietHours(s, e, LocalTime.of(0, 0)))
        assertTrue(NotificationRules.inQuietHours(s, e, LocalTime.of(6, 59)))
        assertFalse(NotificationRules.inQuietHours(s, e, LocalTime.of(7, 0)))
        assertFalse(NotificationRules.inQuietHours(s, e, LocalTime.of(21, 59)))
    }

    @Test
    fun `quiet hours disabled when unset or empty`() {
        assertFalse(NotificationRules.inQuietHours(null, LocalTime.NOON, LocalTime.NOON))
        assertFalse(NotificationRules.inQuietHours(LocalTime.NOON, LocalTime.NOON, LocalTime.NOON))
    }

    @Test
    fun `only pending and partial habits need a reminder`() {
        val today = LocalDate.of(2026, 5, 10)
        fun item(id: String, state: DayState) =
            HabitWithStreak(testHabit(id), emptyMap(), mapOf(today to state), HabitStreak.Empty, today)
        val habits = DayState.entries.map { item(it.name, it) }
        val pending = NotificationRules.pendingToday(habits).map { it.id }.toSet()
        assertEquals(setOf("PENDING", "PARTIAL"), pending)
    }

    @Test
    fun `nudge lists at most three names`() {
        val habits = (1..5).map { testHabit("$it") }
        val (names, more) = NotificationRules.nudgeNames(habits)
        assertEquals(listOf("Habit 1", "Habit 2", "Habit 3"), names)
        assertEquals(2, more)
        assertEquals(0, NotificationRules.nudgeNames(habits.take(2)).second)
    }
}
