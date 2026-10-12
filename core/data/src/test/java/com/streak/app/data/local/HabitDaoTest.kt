package com.streak.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streak.app.data.mapper.reminderEntities
import com.streak.app.data.mapper.toEntity
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.Reminder
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var db: StreakDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), StreakDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    // Regresija: REPLACE na nadrejeni tabeli je sprozil ON DELETE CASCADE in
    // ob vsakem urejanju navade izbrisal vso njeno zgodovino.
    @Test fun editingHabitKeepsItsCompletions() = runTest {
        val habit = testHabit(id = "h1")
        db.habitDao().upsertWithReminders(habit.toEntity(), habit.reminderEntities())
        db.completionDao().upsert(completion(LocalDate.of(2026, 10, 1), habitId = "h1").toEntity())
        db.completionDao().upsert(completion(LocalDate.of(2026, 10, 2), habitId = "h1").toEntity())

        val edited = habit.copy(title = "Branje 20 strani", reminders = listOf(Reminder(time = LocalTime.of(8, 0))))
        db.habitDao().upsertWithReminders(edited.toEntity(), edited.reminderEntities())

        assertEquals("Branje 20 strani", db.habitDao().getById("h1")?.title)
        assertEquals(2, db.completionDao().getForHabit("h1").size)
        assertEquals(1, db.habitDao().getReminders("h1").size)
    }

    @Test fun replacingRemindersRemovesOldOnes() = runTest {
        val habit = testHabit(id = "h1").copy(
            reminders = listOf(Reminder(time = LocalTime.of(7, 0)), Reminder(time = LocalTime.of(20, 0)))
        )
        db.habitDao().upsertWithReminders(habit.toEntity(), habit.reminderEntities())
        val edited = habit.copy(reminders = listOf(Reminder(time = LocalTime.of(9, 30))))
        db.habitDao().upsertWithReminders(edited.toEntity(), edited.reminderEntities())
        assertEquals(listOf(9 * 60 + 30), db.habitDao().getReminders("h1").map { it.minuteOfDay })
    }

    @Test fun oneCompletionPerHabitAndDay() = runTest {
        val habit = testHabit(id = "h1")
        db.habitDao().upsert(habit.toEntity())
        val day = LocalDate.of(2026, 10, 1)
        db.completionDao().upsert(completion(day, habitId = "h1").toEntity())
        db.completionDao().upsert(completion(day, CompletionStatus.SKIPPED, habitId = "h1").copy(id = "other").toEntity())
        val stored = db.completionDao().getForHabit("h1")
        assertEquals(1, stored.size)
        assertEquals(CompletionStatus.SKIPPED, stored.single().status)
    }

    @Test fun deletingHabitCascadesToCompletionsAndReminders() = runTest {
        val habit = testHabit(id = "h1").copy(reminders = listOf(Reminder(time = LocalTime.NOON)))
        db.habitDao().upsertWithReminders(habit.toEntity(), habit.reminderEntities())
        db.completionDao().upsert(completion(LocalDate.of(2026, 10, 1), habitId = "h1").toEntity())
        db.habitDao().delete("h1")
        assertEquals(0, db.completionDao().getForHabit("h1").size)
        assertEquals(0, db.habitDao().getReminders("h1").size)
    }
}
