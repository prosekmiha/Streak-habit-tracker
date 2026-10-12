package com.streak.app.ui.home

import com.streak.app.domain.backup.SnapshotInfo
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.usecase.SetHabitArchivedUseCase
import com.streak.app.domain.usecase.ToggleHabitUseCase
import com.streak.app.testing.FakeBackupService
import com.streak.app.testing.FakeDatabaseHealthMonitor
import com.streak.app.testing.FakeDayClock
import com.streak.app.testing.FakeHabitRepository
import com.streak.app.testing.FakeReminderScheduler
import com.streak.app.testing.FakeSessionController
import com.streak.app.testing.FakeSettingsRepository
import com.streak.app.testing.MainDispatcherRule
import com.streak.app.testing.testHabit
import com.streak.app.testing.withStreak
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class HomeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 7)
    private val clock = FakeDayClock(instant = Instant.parse("2026-10-07T10:00:00Z"))
    private val habits = FakeHabitRepository()
    private val reminders = FakeReminderScheduler()
    private val backup = FakeBackupService()

    private fun viewModel() = HomeViewModel(
        habitRepository = habits,
        settingsRepository = FakeSettingsRepository(),
        dayClock = clock,
        logHabit = ToggleHabitUseCase(habits, clock),
        setArchived = SetHabitArchivedUseCase(habits, reminders),
        sessionController = FakeSessionController(),
        backup = backup,
        databaseHealth = FakeDatabaseHealthMonitor()
    )

    // state se deli z WhileSubscribed: brez zbiralca se ne izracuna
    private fun TestScope.observed(vm: HomeViewModel) =
        vm.also { backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.state.collect {} } }

    @Test
    fun `counts due and done habits for today`() = runTest {
        habits.withStreak.value = listOf(
            withStreak(testHabit("a"), today, days = mapOf(today to DayState.DONE)),
            withStreak(testHabit("b"), today, days = mapOf(today to DayState.PENDING)),
            withStreak(testHabit("c"), today, days = mapOf(today to DayState.NOT_SCHEDULED))
        )

        val state = observed(viewModel()).state.value

        assertTrue(state.loaded)
        assertEquals(2, state.dueCount)
        assertEquals(1, state.doneCount)
        assertEquals(listOf("a", "b"), state.sections.flatMap { it.items }.map { it.habit.id })
    }

    @Test
    fun `tapping a yes-no habit logs it as done`() = runTest {
        val habit = testHabit("a")
        habits.upsertHabit(habit)
        habits.withStreak.value = listOf(withStreak(habit, today))
        val vm = observed(viewModel())

        vm.onTap(vm.state.value.item("a")!!)

        assertEquals(CompletionStatus.DONE, habits.getCompletion("a", today)?.status)
    }

    @Test
    fun `tapping a duration habit opens the session instead of logging`() = runTest {
        val habit = testHabit("d", goalType = GoalType.DURATION, target = 600.0)
        habits.withStreak.value = listOf(withStreak(habit, today))
        val vm = observed(viewModel())

        vm.onTap(vm.state.value.item("d")!!)

        assertEquals(HomeEvent.OpenSession("d"), vm.eventFlow.first())
        assertNull(habits.getCompletion("d", today))
    }

    @Test
    fun `archiving from home also cancels reminders`() = runTest {
        val habit = testHabit("a")
        habits.upsertHabit(habit)

        viewModel().archive(habit)

        assertTrue(habits.getHabit("a")!!.isArchived)
        assertEquals(listOf("a"), reminders.cancelled)
    }

    @Test
    fun `offers snapshot restore and restores on accept`() = runTest {
        backup.snapshot = SnapshotInfo(createdAt = null, habits = 3)
        val vm = viewModel()

        assertEquals(3, vm.restoreOffer.value?.habits)

        vm.acceptRestore()

        assertNull(vm.restoreOffer.value)
        assertEquals(1, backup.restored)
    }
}
