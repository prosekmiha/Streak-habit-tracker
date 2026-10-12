package com.streak.app.domain.review

import com.streak.app.domain.gamification.DayActivity
import com.streak.app.domain.gamification.DaySessions
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.MoodEntry
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class ReviewCalculatorTest {

    private val october = YearMonth.of(2026, 10)
    private val range = ReviewRange.month(october)
    private fun day(d: Int) = october.atDay(d)

    private fun input(
        today: LocalDate = day(31),
        habits: List<com.streak.app.domain.model.Habit> = emptyList(),
        completions: List<com.streak.app.domain.model.Completion> = emptyList(),
        aggregates: List<DailyAggregate> = emptyList(),
        activity: List<DayActivity> = emptyList(),
        sessions: List<DaySessions> = emptyList(),
        moods: List<MoodEntry> = emptyList(),
        archivedOn: Map<String, LocalDate> = emptyMap()
    ) = ReviewInput(habits, completions, aggregates, activity, sessions, moods, archivedOn, today)

    @Test fun `ranges move by month and year`() {
        assertEquals(LocalDate.of(2026, 9, 1), range.previous().start)
        assertEquals(LocalDate.of(2026, 11, 30), range.next().end)
        val y = ReviewRange.year(Year.of(2026))
        assertEquals(LocalDate.of(2026, 12, 31), y.end)
        assertEquals(ReviewRange.year(Year.of(2025)), y.previous())
    }

    @Test fun `empty period`() {
        val r = ReviewCalculator.compute(range, input())
        assertTrue(r.isEmpty)
        assertNull(r.topHabit)
        assertNull(r.chronotype)
    }

    @Test fun `counts done habits, top habit and ignores other months`() {
        val run = testHabit("run")
        val read = testHabit("read")
        val completions = listOf(
            completion(day(1), habitId = "run"), completion(day(2), habitId = "run"), completion(day(3), habitId = "run"),
            completion(day(1), habitId = "read"),
            completion(day(4), CompletionStatus.SKIPPED, habitId = "read"),
            completion(LocalDate.of(2026, 9, 30), habitId = "read")
        )
        val r = ReviewCalculator.compute(range, input(habits = listOf(run, read), completions = completions))
        assertEquals(4, r.doneCount)
        assertEquals(run, r.topHabit)
        assertEquals(3, r.topHabitDays)
    }

    @Test fun `perfect days, longest run and best weekday from aggregates`() {
        // 5.-7. 10. popolni (pon-sre), 8. polovicen, 9. popoln
        val aggregates = listOf(
            DailyAggregate(day(5), 2, 2), DailyAggregate(day(6), 2, 2), DailyAggregate(day(7), 2, 2),
            DailyAggregate(day(8), 2, 1), DailyAggregate(day(9), 2, 2)
        )
        val r = ReviewCalculator.compute(range, input(aggregates = aggregates))
        assertEquals(4, r.perfectDays)
        assertEquals(3, r.longestPerfectRun)
        assertEquals(5, r.activeDays)
        // Najvisji delez imajo pon, tor, sre in pet (1.0): ob enakosti zmaga zgodnejsi dan
        assertEquals(DayOfWeek.MONDAY, r.bestWeekday)
    }

    @Test fun `only days up to today count`() {
        val quit = testHabit("q", goalType = GoalType.QUIT, target = 0.0, start = day(1))
        val r = ReviewCalculator.compute(range, input(today = day(10), habits = listOf(quit)))
        assertEquals(10, r.cleanDays)
        assertEquals(10, r.doneCount)
    }

    @Test fun `quit habit clean days skip slips and stop at archiving`() {
        val quit = testHabit("q", goalType = GoalType.QUIT, target = 0.0, start = day(1))
        val slips = listOf(completion(day(3), CompletionStatus.RELAPSE, habitId = "q", value = 1.0))
        val r = ReviewCalculator.compute(range, input(habits = listOf(quit), completions = slips, archivedOn = mapOf("q" to day(20))))
        assertEquals(19, r.cleanDays)
        // Zdrs ni opravljena navada
        assertNull(r.topHabit)
    }

    @Test fun `chronotype needs enough logs`() {
        fun act(total: Int, before9: Int, before18: Int) = listOf(DayActivity(day(1), total, before9, before18, 0))
        assertNull(ReviewCalculator.compute(range, input(activity = act(4, 4, 4))).chronotype)
        assertEquals(Chronotype.EARLY_BIRD, ReviewCalculator.compute(range, input(activity = act(10, 6, 8))).chronotype)
        assertEquals(Chronotype.DAYTIME, ReviewCalculator.compute(range, input(activity = act(10, 2, 7))).chronotype)
        assertEquals(Chronotype.NIGHT_OWL, ReviewCalculator.compute(range, input(activity = act(10, 1, 3))).chronotype)
    }

    @Test fun `sessions and mood`() {
        val r = ReviewCalculator.compute(
            range,
            input(
                sessions = listOf(DaySessions(day(2), 2, 1800, 6), DaySessions(day(3), 1, 600, 2)),
                moods = listOf(MoodEntry(day(1), 4, loggedAt = Instant.EPOCH), MoodEntry(day(2), 2, loggedAt = Instant.EPOCH))
            )
        )
        assertEquals(3, r.sessionCount)
        assertEquals(2400L, r.sessionSeconds)
        assertEquals(3.0, r.moodAverage!!, 1e-9)
        assertEquals(2, r.moodEntries)
    }
}
