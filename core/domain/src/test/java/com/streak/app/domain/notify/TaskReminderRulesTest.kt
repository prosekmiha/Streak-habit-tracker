package com.streak.app.domain.notify

import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskRecurrence
import com.streak.app.domain.model.TaskReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskReminderRulesTest {

    private val ljubljana = ZoneId.of("Europe/Ljubljana")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), ljubljana)
    private fun date(text: String) = LocalDate.parse(text)

    // 2026-05-11 je ponedeljek
    private fun task(
        due: String? = "2026-05-11",
        dueTime: LocalTime? = LocalTime.of(14, 0),
        reminders: List<TaskReminder>,
        recurring: Set<DayOfWeek>? = null,
        done: Boolean = false
    ) = Task(
        id = "t", title = "Naloga", dueDate = due?.let(LocalDate::parse),
        dueMinuteOfDay = dueTime?.let { it.hour * 60 + it.minute }, priority = Priority.NORMAL, isDone = done,
        isRecurring = recurring != null, recurrenceRule = recurring?.let(TaskRecurrence::build), createdAt = 0,
        reminders = reminders
    )

    private fun next(task: Task, now: String) = TaskReminderRules.nextTrigger(task, at(now))

    @Test
    fun `before due fires the given minutes before the due time`() {
        val t = task(reminders = listOf(TaskReminder.BeforeDue(60)))
        assertEquals(TaskTrigger(at("2026-05-11T13:00"), date("2026-05-11")), next(t, "2026-05-10T09:00"))
    }

    @Test
    fun `earliest of several reminders wins`() {
        val t = task(reminders = listOf(TaskReminder.BeforeDue(0), TaskReminder.DayAt(1, LocalTime.of(20, 0))))
        assertEquals(at("2026-05-10T20:00"), next(t, "2026-05-10T09:00")?.at)
        assertEquals(at("2026-05-11T14:00"), next(t, "2026-05-10T20:00")?.at)
    }

    @Test
    fun `before due without a due time is ignored`() {
        val t = task(dueTime = null, reminders = listOf(TaskReminder.BeforeDue(30)))
        assertNull(next(t, "2026-05-10T09:00"))
    }

    @Test
    fun `day at reminders work without a due time`() {
        val t = task(dueTime = null, reminders = listOf(TaskReminder.DayAt(0, LocalTime.of(8, 0))))
        assertEquals(at("2026-05-11T08:00"), next(t, "2026-05-10T09:00")?.at)
    }

    @Test
    fun `one time task has no reminders after its due time`() {
        val t = task(reminders = listOf(TaskReminder.BeforeDue(0)))
        assertNull(next(t, "2026-05-11T14:00"))
        assertNull(next(t, "2026-05-12T08:00"))
    }

    @Test
    fun `repeat fires every interval inside the window on the due day`() {
        val t = task(reminders = listOf(TaskReminder.Repeat(90, LocalTime.of(9, 0), LocalTime.of(12, 0))))
        assertEquals(at("2026-05-11T09:00"), next(t, "2026-05-10T22:00")?.at)
        assertEquals(at("2026-05-11T10:30"), next(t, "2026-05-11T09:00")?.at)
        assertEquals(at("2026-05-11T12:00"), next(t, "2026-05-11T11:00")?.at)
        assertNull(next(t, "2026-05-11T12:00"))
    }

    @Test
    fun `repeat with an empty window never fires`() {
        val t = task(reminders = listOf(TaskReminder.Repeat(60, LocalTime.of(12, 0), LocalTime.of(9, 0))))
        assertNull(next(t, "2026-05-10T22:00"))
    }

    @Test
    fun `done task or task without date has no trigger`() {
        assertNull(next(task(done = true, reminders = listOf(TaskReminder.BeforeDue(0))), "2026-05-10T09:00"))
        assertNull(next(task(due = null, reminders = listOf(TaskReminder.DayAt(0, LocalTime.NOON))), "2026-05-10T09:00"))
    }

    @Test
    fun `daily task reminds for every occurrence from the due date on`() {
        val t = task(reminders = listOf(TaskReminder.BeforeDue(0)), recurring = emptySet())
        assertEquals(TaskTrigger(at("2026-05-11T14:00"), date("2026-05-11")), next(t, "2026-05-01T09:00"))
        assertEquals(TaskTrigger(at("2026-05-21T14:00"), date("2026-05-21")), next(t, "2026-05-20T15:00"))
    }

    @Test
    fun `weekly task uses only chosen weekdays`() {
        val t = task(
            reminders = listOf(TaskReminder.DayAt(1, LocalTime.of(20, 0))),
            recurring = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        )
        // Po ponedeljku: naslednji termin je cetrtek, opomnik v sredo zvecer
        assertEquals(TaskTrigger(at("2026-05-13T20:00"), date("2026-05-14")), next(t, "2026-05-11T10:00"))
        // V sredo ob 20:00: naslednji je ponedeljek, opomnik v nedeljo
        assertEquals(TaskTrigger(at("2026-05-17T20:00"), date("2026-05-18")), next(t, "2026-05-13T20:00"))
    }

    @Test
    fun `days before can trigger before a later occurrence`() {
        val t = task(due = "2026-05-15", reminders = listOf(TaskReminder.DayAt(3, LocalTime.of(9, 0))), recurring = setOf(DayOfWeek.FRIDAY))
        assertEquals(TaskTrigger(at("2026-05-12T09:00"), date("2026-05-15")), next(t, "2026-05-11T09:00"))
        assertEquals(TaskTrigger(at("2026-05-19T09:00"), date("2026-05-22")), next(t, "2026-05-12T09:00"))
    }

    @Test
    fun `time in the spring dst gap moves forward`() {
        // 2026-03-29 02:30 v Ljubljani ne obstaja
        val t = task(due = "2026-03-29", dueTime = null, reminders = listOf(TaskReminder.DayAt(0, LocalTime.of(2, 30))))
        assertEquals(at("2026-03-29T03:30"), next(t, "2026-03-28T22:00")?.at)
    }
}
