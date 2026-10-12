package com.streak.app.domain.mood

import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.MoodEntry
import com.streak.app.testing.completion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MoodInsightsTest {

    private val start = LocalDate.of(2026, 10, 1)
    private fun day(i: Int) = start.plusDays(i.toLong())
    private fun mood(i: Int, level: Int) = MoodEntry(day(i), level, loggedAt = Instant.EPOCH)

    @Test fun `no entries gives empty stats`() {
        assertEquals(MoodStats.Empty, MoodInsights.compute(emptyList(), emptyList(), emptyList()))
    }

    @Test fun `average and correlation with completed days`() {
        // Dnevi 0-2: vse opravljeno, razpolozenje 5; dnevi 3-5: polovica, razpolozenje 2
        val moods = (0..2).map { mood(it, 5) } + (3..5).map { mood(it, 2) }
        val aggregates = (0..2).map { DailyAggregate(day(it), 4, 4) } + (3..5).map { DailyAggregate(day(it), 4, 2) }
        val s = MoodInsights.compute(moods, aggregates, emptyList())
        assertEquals(6, s.entries)
        assertEquals(3.5, s.average!!, 1e-9)
        assertTrue(s.hasCorrelation)
        assertEquals(5.0, s.goodDaysAverage!!, 1e-9)
        assertEquals(2.0, s.otherDaysAverage!!, 1e-9)
    }

    @Test fun `correlation needs enough days on both sides`() {
        val moods = (0..3).map { mood(it, 4) }
        val aggregates = (0..2).map { DailyAggregate(day(it), 2, 2) } + DailyAggregate(day(3), 2, 0)
        val s = MoodInsights.compute(moods, aggregates, emptyList())
        assertFalse(s.hasCorrelation)
        assertNull(s.goodDaysAverage)
    }

    @Test fun `top habit is the one with the highest mood on its done days`() {
        val moods = listOf(mood(0, 5), mood(1, 5), mood(2, 4), mood(3, 1), mood(4, 2), mood(5, 1))
        val completions = (0..2).map { completion(day(it), habitId = "run") } +
            (3..5).map { completion(day(it), habitId = "tv") } +
            completion(day(1), CompletionStatus.SKIPPED, habitId = "skip")
        val s = MoodInsights.compute(moods, emptyList(), completions)
        assertEquals("run", s.topHabitId)
        assertEquals(14.0 / 3, s.topHabitAverage!!, 1e-9)
    }

    @Test fun `no top habit when none is above average`() {
        val moods = (0..3).map { mood(it, 3) }
        val completions = (0..3).map { completion(day(it), habitId = "a") }
        assertNull(MoodInsights.compute(moods, emptyList(), completions).topHabitId)
    }
}
