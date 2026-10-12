package com.streak.app.domain.usecase

import com.streak.app.testing.FakeHabitRepository
import com.streak.app.testing.FakeReminderScheduler
import com.streak.app.testing.FakeSessionController
import com.streak.app.testing.testHabit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitLifecycleUseCasesTest {

    private val repository = FakeHabitRepository()
    private val reminders = FakeReminderScheduler()

    @Test
    fun `save stores habit and reschedules its reminders`() = runTest {
        SaveHabitUseCase(repository, reminders)(testHabit("a"))

        assertEquals(testHabit("a"), repository.getHabit("a"))
        // Stari casi se najprej preklicejo, nato se razporedijo novi
        assertEquals(listOf("a"), reminders.cancelled)
        assertEquals(listOf("a"), reminders.scheduled)
    }

    @Test
    fun `archiving cancels reminders and unarchiving schedules them again`() = runTest {
        repository.upsertHabit(testHabit("a"))
        val setArchived = SetHabitArchivedUseCase(repository, reminders)

        setArchived("a", archived = true)
        assertTrue(repository.getHabit("a")!!.isArchived)
        assertEquals(listOf("a"), reminders.cancelled)
        assertTrue(reminders.scheduled.isEmpty())

        setArchived("a", archived = false)
        assertFalse(repository.getHabit("a")!!.isArchived)
        assertEquals(listOf("a"), reminders.scheduled)
    }

    @Test
    fun `delete removes habit and cancels reminders`() = runTest {
        repository.upsertHabit(testHabit("a"))

        DeleteHabitUseCase(repository, reminders, FakeSessionController())("a")

        assertNull(repository.getHabit("a"))
        assertEquals(listOf("a"), reminders.cancelled)
    }

    @Test
    fun `delete discards a running session of that habit only`() = runTest {
        repository.upsertHabit(testHabit("a"))
        repository.upsertHabit(testHabit("b"))
        val sessions = FakeSessionController()
        sessions.start(testHabit("a"))

        DeleteHabitUseCase(repository, reminders, sessions)("b")
        assertEquals("a", sessions.active.value?.habitId)

        DeleteHabitUseCase(repository, reminders, sessions)("a")
        assertNull(sessions.active.value)
    }
}
