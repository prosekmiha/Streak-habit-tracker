package com.streak.app.ui.home

import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.testing.testHabit
import com.streak.app.testing.withStreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MilestoneTrackerTest {

    private val today = LocalDate.of(2026, 10, 7)
    private val tracker = MilestoneTracker()

    private fun habitWith(streak: Int) = listOf(withStreak(testHabit("a"), today, streak = HabitStreak(streak, streak, 1f)))

    @Test
    fun `celebrates once when a check-in reaches a milestone`() {
        tracker.expect("a", streakBefore = 6)

        // Streak se se ni preracunal: pocakaj
        assertNull(tracker.check(habitWith(6)))
        assertEquals(Celebration("Habit a", 7, StreakUnit.DAY), tracker.check(habitWith(7)))

        // Isti mejnik se ne praznuje dvakrat
        tracker.expect("a", streakBefore = 6)
        assertNull(tracker.check(habitWith(7)))
    }

    @Test
    fun `ignores streaks that are not milestones or did not grow`() {
        tracker.expect("a", streakBefore = 7)
        assertNull(tracker.check(habitWith(8)))

        tracker.expect("a", streakBefore = 31)
        assertNull(tracker.check(habitWith(30)))
    }

    @Test
    fun `without a check-in nothing is celebrated`() {
        assertNull(tracker.check(habitWith(7)))
    }
}
