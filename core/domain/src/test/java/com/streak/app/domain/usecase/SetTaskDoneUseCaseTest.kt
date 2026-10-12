package com.streak.app.domain.usecase

import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskRecurrence
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.TaskRepository
import com.streak.app.testing.FakeDayClock
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class SetTaskDoneUseCaseTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val reminders = mockk<TaskReminderScheduler>(relaxed = true)

    // 2026-10-07 je sreda
    private val clock = FakeDayClock(instant = Instant.parse("2026-10-07T10:00:00Z"))
    private val useCase = SetTaskDoneUseCase(repository, reminders, clock)

    private fun task(due: String?, recurring: Set<DayOfWeek>? = null) = Task(
        id = "t", title = "Naloga", dueDate = due?.let(LocalDate::parse), dueMinuteOfDay = null,
        priority = Priority.NORMAL, isDone = false, isRecurring = recurring != null,
        recurrenceRule = recurring?.let(TaskRecurrence::build), createdAt = 0
    )

    @Test
    fun `one-off task is marked done`() = runTest {
        useCase(task("2026-10-07"), done = true)
        coVerify { repository.setDone("t", true) }
        verify { reminders.schedule(match { it.isDone }) }
    }

    @Test
    fun `daily task moves to tomorrow instead of being finished`() = runTest {
        useCase(task("2026-10-07", recurring = emptySet()), done = true)
        coVerify(exactly = 0) { repository.setDone(any(), any()) }
        coVerify { repository.upsertTask(match { !it.isDone && it.dueDate == LocalDate.parse("2026-10-08") }) }
        verify { reminders.cancel("t") }
        verify { reminders.schedule(match { it.dueDate == LocalDate.parse("2026-10-08") }) }
    }

    @Test
    fun `overdue weekly task moves to the next occurrence after today`() = runTest {
        // Ponedeljki; zamujen termin 2026-09-28, danes sreda 2026-10-07 -> ponedeljek 2026-10-12
        useCase(task("2026-09-28", recurring = setOf(DayOfWeek.MONDAY)), done = true)
        coVerify { repository.upsertTask(match { it.dueDate == LocalDate.parse("2026-10-12") }) }
    }

    @Test
    fun `task completed early moves past its own due date`() = runTest {
        // Rok v petek 2026-10-09, odkljukan v sredo -> naslednji petek
        useCase(task("2026-10-09", recurring = setOf(DayOfWeek.FRIDAY)), done = true)
        coVerify { repository.upsertTask(match { it.dueDate == LocalDate.parse("2026-10-16") }) }
    }

    @Test
    fun `next occurrence respects due date and weekdays`() {
        val weekly = task("2026-10-07", recurring = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertEquals(LocalDate.parse("2026-10-08"), weekly.nextOccurrence(LocalDate.parse("2026-10-01")))
        assertEquals(LocalDate.parse("2026-10-12"), weekly.nextOccurrence(LocalDate.parse("2026-10-09")))
        val once = task("2026-10-07")
        assertEquals(LocalDate.parse("2026-10-07"), once.nextOccurrence(LocalDate.parse("2026-10-07")))
        assertNull(once.nextOccurrence(LocalDate.parse("2026-10-08")))
        assertNull(task(null, recurring = emptySet()).nextOccurrence(LocalDate.parse("2026-10-07")))
    }
}
