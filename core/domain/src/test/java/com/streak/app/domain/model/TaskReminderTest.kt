package com.streak.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class TaskReminderTest {

    @Test
    fun `encode and decode round trip`() {
        val reminders = listOf(
            TaskReminder.BeforeDue(0),
            TaskReminder.BeforeDue(1440),
            TaskReminder.DayAt(1, LocalTime.of(20, 0)),
            TaskReminder.Repeat(30, LocalTime.of(9, 15), LocalTime.of(21, 0))
        )
        val text = TaskReminder.encode(reminders)
        assertEquals("B0;B1440;D1@1200;R30@555-1260", text)
        assertEquals(reminders, TaskReminder.decode(text))
    }

    @Test
    fun `decode skips invalid parts`() {
        assertEquals(listOf(TaskReminder.BeforeDue(15)), TaskReminder.decode("X1;B15;D1;R"))
        assertEquals(emptyList<TaskReminder>(), TaskReminder.decode(""))
        assertEquals(emptyList<TaskReminder>(), TaskReminder.decode(null))
    }

    @Test
    fun `recurrence rule round trip`() {
        assertEquals("FREQ=DAILY", TaskRecurrence.build(emptySet()))
        assertEquals("FREQ=DAILY", TaskRecurrence.build(DayOfWeek.entries.toSet()))
        val rule = TaskRecurrence.build(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY))
        assertEquals("FREQ=WEEKLY;BYDAY=MO,WE", rule)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), TaskRecurrence.parseDays(rule))
        assertEquals(emptySet<DayOfWeek>(), TaskRecurrence.parseDays("FREQ=DAILY"))
    }
}
