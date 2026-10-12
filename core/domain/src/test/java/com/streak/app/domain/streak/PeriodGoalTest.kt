package com.streak.app.domain.streak

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.ScheduleType
import com.streak.app.testing.completion
import com.streak.app.testing.completions
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodGoalTest {

    // 2026-10-07 je sreda; teden se zacne v ponedeljek 2026-10-05
    private val today = LocalDate.of(2026, 10, 7)
    private val weekly = testHabit(type = ScheduleType.TIMES_PER_WEEK, times = 2)

    @Test
    fun `weekly goal is met once enough days are done this week`() {
        val oneDone = completions(completion(LocalDate.of(2026, 10, 5)))
        val twoDone = completions(completion(LocalDate.of(2026, 10, 5)), completion(LocalDate.of(2026, 10, 6)))
        assertFalse(PeriodGoal.isMet(weekly, oneDone, emptyList(), today, DayOfWeek.MONDAY))
        assertTrue(PeriodGoal.isMet(weekly, twoDone, emptyList(), today, DayOfWeek.MONDAY))
        // Zadnji teden ne steje v tekoci teden
        val lastWeek = completions(completion(LocalDate.of(2026, 10, 1)), completion(LocalDate.of(2026, 10, 2)))
        assertFalse(PeriodGoal.isMet(weekly, lastWeek, emptyList(), today, DayOfWeek.MONDAY))
    }

    @Test
    fun `today is no longer pending once the period goal is met`() {
        assertEquals(DayState.PENDING, DayEvaluator.stateOn(weekly, today, null, emptyList(), today))
        assertEquals(DayState.NOT_SCHEDULED, DayEvaluator.stateOn(weekly, today, null, emptyList(), today, periodGoalMet = true))
    }

    @Test
    fun `daily habits never count as period goal met`() {
        val daily = testHabit()
        assertFalse(PeriodGoal.isMet(daily, completions(completion(today)), emptyList(), today, DayOfWeek.MONDAY))
    }

    @Test
    fun `monthly goal larger than the month can still be met`() {
        val habit = testHabit(type = ScheduleType.TIMES_PER_MONTH, times = 30)
        val feb = (1..28).map { completion(LocalDate.of(2026, 2, it)) }.toTypedArray()
        assertTrue(PeriodGoal.isMet(habit, completions(*feb), emptyList(), LocalDate.of(2026, 2, 28), DayOfWeek.MONDAY))
    }
}
