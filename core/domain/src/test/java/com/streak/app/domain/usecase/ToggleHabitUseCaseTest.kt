package com.streak.app.domain.usecase

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.repository.HabitRepository
import com.streak.app.testing.FakeDayClock
import com.streak.app.testing.testHabit
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ToggleHabitUseCaseTest {

    // Shramba v pomnilniku namesto Room
    private val store = HashMap<Pair<String, LocalDate>, Completion>()
    private val repository = mockk<HabitRepository>(relaxed = true)
    private val clock = FakeDayClock(instant = Instant.parse("2026-10-07T10:00:00Z"))
    private val useCase = ToggleHabitUseCase(repository, clock)
    private val today = LocalDate.of(2026, 10, 7)

    @Before fun setUp() {
        val saved = slot<Completion>()
        coEvery { repository.getCompletion(any(), any()) } answers { store[firstArg<String>() to secondArg<LocalDate>()] }
        coEvery { repository.saveCompletion(capture(saved)) } answers { store[saved.captured.habitId to saved.captured.date] = saved.captured }
        coEvery { repository.deleteCompletion(any(), any()) } answers { store.remove(firstArg<String>() to secondArg<LocalDate>()) }
    }

    @Test fun `toggle marks done and toggles back`() = runTest {
        val habit = testHabit()
        assertEquals(LogResult.OK, useCase(habit, today))
        val c = store[habit.id to today]!!
        assertEquals(CompletionStatus.DONE, c.status)
        assertFalse(c.manuallyEdited)
        assertEquals(clock.instant, c.loggedAt)
        assertEquals(2 * 3600, c.zoneOffset.totalSeconds) // CEST

        useCase(habit, today)
        assertNull(store[habit.id to today])
    }

    @Test fun `future days are blocked`() = runTest {
        assertEquals(LogResult.FUTURE_BLOCKED, useCase(testHabit(), today.plusDays(1)))
        assertTrue(store.isEmpty())
    }

    @Test fun `backfill is marked as manually edited`() = runTest {
        useCase(testHabit(), today.minusDays(3))
        assertTrue(store.values.single().manuallyEdited)
    }

    @Test fun `late night completion goes to the previous logical day`() = runTest {
        clock.dayEndsAt = LocalTime.of(4, 0)
        clock.set(Instant.parse("2026-10-07T23:30:00Z")) // 01:30 v Ljubljani, 8.10.
        val logical = clock.today()
        assertEquals(today, logical)
        useCase(testHabit(), logical)
        assertFalse(store.values.single().manuallyEdited)
    }

    @Test fun `quantity progress accumulates from partial to done`() = runTest {
        val habit = testHabit(goalType = GoalType.QUANTITY, target = 8.0)
        useCase.addProgress(habit, today, 3.0)
        assertEquals(CompletionStatus.PARTIAL, store[habit.id to today]!!.status)
        useCase.addProgress(habit, today, 5.0)
        val done = store[habit.id to today]!!
        assertEquals(CompletionStatus.DONE, done.status)
        assertEquals(8.0, done.value, 0.0)
        useCase.addProgress(habit, today, -1.0)
        assertEquals(CompletionStatus.PARTIAL, store[habit.id to today]!!.status)
        useCase.addProgress(habit, today, -10.0)
        assertNull(store[habit.id to today])
    }

    @Test fun `skip keeps the note of an existing record`() = runTest {
        val habit = testHabit()
        useCase(habit, today)
        store[habit.id to today] = store[habit.id to today]!!.copy(note = "dobro")
        useCase.setStatus(habit, today, CompletionStatus.SKIPPED)
        val c = store[habit.id to today]!!
        assertEquals(CompletionStatus.SKIPPED, c.status)
        assertEquals("dobro", c.note)
    }

    @Test fun `quit - tap does nothing, slips are counted and undone`() = runTest {
        val habit = testHabit(goalType = GoalType.QUIT, target = 0.0)
        assertEquals(LogResult.OK, useCase(habit, today))
        assertNull(store[habit.id to today])

        useCase.addRelapse(habit, today, 1)
        useCase.addRelapse(habit, today, 1)
        val c = store[habit.id to today]!!
        assertEquals(CompletionStatus.RELAPSE, c.status)
        assertEquals(2.0, c.value, 0.0)

        useCase.addRelapse(habit, today, -1)
        assertEquals(1.0, store[habit.id to today]!!.value, 0.0)
        useCase.addRelapse(habit, today, -1)
        assertNull(store[habit.id to today])
    }

    @Test fun `quit - slip cannot be logged for the future`() = runTest {
        val habit = testHabit(goalType = GoalType.QUIT, target = 0.0)
        assertEquals(LogResult.FUTURE_BLOCKED, useCase.addRelapse(habit, today.plusDays(1), 1))
    }
}
