package com.streak.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streak.app.data.local.StreakDatabase
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Vacation
import com.streak.app.domain.usecase.ComputeStreakUseCase
import com.streak.app.testing.FakeDayClock
import com.streak.app.testing.FakeSettingsRepository
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HabitRepositoryImplTest {

    private lateinit var db: StreakDatabase
    private lateinit var repository: HabitRepositoryImpl
    private val clock = FakeDayClock(instant = Instant.parse("2026-10-07T10:00:00Z"))
    private val today = LocalDate.of(2026, 10, 7)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), StreakDatabase::class.java)
            .allowMainThreadQueries().build()
        val settings = FakeSettingsRepository()
        val compute = ComputeStreakUseCase()
        repository = HabitRepositoryImpl(db, StreakRecalculator(db, compute, clock, settings), compute, clock, settings)
    }

    @After fun tearDown() = db.close()

    @Test fun completionsUpdateStreakCacheAndAggregates() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(5)))
        repository.upsertHabit(testHabit(id = "b", start = today.minusDays(5)))
        for (d in 0L..2L) repository.saveCompletion(completion(today.minusDays(d), habitId = "a"))
        repository.saveCompletion(completion(today, habitId = "b"))

        val habits = repository.observeHabits().first().associateBy { it.habit.id }
        assertEquals(3, habits.getValue("a").streak.currentStreak)
        assertEquals(1, habits.getValue("b").streak.currentStreak)
        assertEquals(DayState.DONE, habits.getValue("a").todayState)
        assertEquals(DayState.MISSED, habits.getValue("b").stateOn(today.minusDays(1)))

        val aggregates = repository.observeAggregates(today.minusDays(5), today).first().associateBy { it.date }
        assertEquals(2, aggregates.getValue(today).done)
        assertEquals(2, aggregates.getValue(today).scheduled)
        assertEquals(1, aggregates.getValue(today.minusDays(1)).done)
        assertEquals(2, aggregates.getValue(today.minusDays(1)).scheduled)
    }

    @Test fun deletingCompletionRecomputesStreak() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(5)))
        repository.saveCompletion(completion(today, habitId = "a"))
        repository.saveCompletion(completion(today.minusDays(1), habitId = "a"))
        repository.deleteCompletion("a", today.minusDays(1))
        assertEquals(1, repository.observeHabits().first().single().streak.currentStreak)
    }

    @Test fun dayChangeRefreshesCurrentStreak() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(5)))
        repository.saveCompletion(completion(today, habitId = "a"))
        // Dva dni kasneje: vceraj je bil zgresen -> veriga je prekinjena
        clock.set(Instant.parse("2026-10-09T10:00:00Z"))
        StreakRecalculator(db, ComputeStreakUseCase(), clock, FakeSettingsRepository()).onDayChanged()
        val item = repository.observeHabits().first().single()
        assertEquals(0, item.streak.currentStreak)
        assertEquals(1, item.streak.bestStreak)
    }

    @Test fun vacationRecalculatesEverything() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(5)))
        repository.saveCompletion(completion(today, habitId = "a"))
        repository.saveCompletion(completion(today.minusDays(3), habitId = "a"))
        assertEquals(1, repository.observeHabits().first().single().streak.currentStreak)
        repository.addVacation(Vacation(startDate = today.minusDays(2), endDate = today.minusDays(1)))
        assertEquals(2, repository.observeHabits().first().single().streak.currentStreak)
    }

    @Test fun archivedHabitsAreHiddenButKeepHistory() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(5)))
        repository.saveCompletion(completion(today.minusDays(1), habitId = "a"))
        repository.setArchived("a", true)
        assertTrue(repository.observeHabits().first().isEmpty())
        assertEquals(1, repository.observeArchivedHabits().first().size)
        assertEquals(1, repository.getCompletionsBetween(today.minusDays(5), today).size)
        repository.setArchived("a", false)
        assertNotNull(repository.observeHabits().first().singleOrNull())
    }

    @Test fun habitDetailContainsFullHistory() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(400)))
        repository.saveCompletion(completion(today.minusDays(399), habitId = "a"))
        repository.saveCompletion(completion(today.minusDays(1), CompletionStatus.SKIPPED, habitId = "a"))
        val detail = repository.observeHabit("a").first()!!
        assertEquals(DayState.DONE, detail.stateOn(today.minusDays(399)))
        assertEquals(DayState.SKIPPED, detail.stateOn(today.minusDays(1)))
        assertEquals(1, detail.streak.bestStreak)
    }

    @Test fun recalculateAllRebuildsCaches() = runTest {
        repository.upsertHabit(testHabit(id = "a", start = today.minusDays(5)))
        repository.saveCompletion(completion(today, habitId = "a"))
        db.statsDao().replaceAggregates(0, Long.MAX_VALUE / 2, emptyList())
        repository.recalculateAll()
        assertEquals(1, repository.observeAggregates(today, today).first().single().done)
    }
}
