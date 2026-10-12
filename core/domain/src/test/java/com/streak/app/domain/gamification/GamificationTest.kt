package com.streak.app.domain.gamification

import com.streak.app.domain.model.DailyAggregate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GamificationTest {

    private val today = LocalDate.of(2026, 10, 11)

    private fun input(
        activity: List<DayActivity> = emptyList(),
        sessions: List<DaySessions> = emptyList(),
        aggregates: List<DailyAggregate> = emptyList(),
        bestStreak: Int = 0
    ) = GameInput(activity, sessions, aggregates, bestStreak, today)

    @Test
    fun `level thresholds grow`() {
        assertEquals(1, Levels.of(0).level)
        assertEquals(1, Levels.of(99).level)
        assertEquals(2, Levels.of(100).level)
        assertEquals(3, Levels.of(300).level)
        assertEquals(5, Levels.of(1000).level)
        assertEquals(10, Levels.of(4500).level)
        val l = Levels.of(150)
        assertEquals(50, l.xpIntoLevel)
        assertEquals(200, l.xpForLevel)
        assertEquals(0.25f, l.progress)
    }

    @Test
    fun `xp from completions, perfect days and sessions`() {
        // Dan, ki ni izpolnil nobenega izziva (izziv je odvisen od datuma)
        val day = (0L..30L).map { today.minusDays(it) }.first { Challenge.forDate(it) == Challenge.REFLECT }
        val state = Gamification.compute(
            input(
                activity = listOf(DayActivity(day, done = 3, doneBefore9 = 0, doneBefore18 = 0, notes = 0)),
                sessions = listOf(DaySessions(day, count = 2, seconds = 1500, units = 5)),
                aggregates = listOf(DailyAggregate(day, scheduled = 3, done = 3))
            )
        )
        assertEquals(30, state.xp.habits)
        assertEquals(20, state.xp.perfectDays)
        assertEquals(10, state.xp.sessions)
        assertEquals(0, state.xp.challenges)
        assertEquals(60, state.xp.total)
    }

    @Test
    fun `undoing a completion removes its xp`() {
        val a = DailyAggregate(today, scheduled = 2, done = 2)
        val before = Gamification.compute(input(listOf(DayActivity(today, 2, 0, 0, 0)), aggregates = listOf(a)))
        val after = Gamification.compute(input(listOf(DayActivity(today, 1, 0, 0, 0)), aggregates = listOf(a.copy(done = 1))))
        assertTrue(after.xp.total < before.xp.total)
        assertEquals(10, after.xp.habits)
        assertEquals(0, after.xp.perfectDays)
    }

    @Test
    fun `challenges rotate and never repeat on consecutive days`() {
        val days = (0L until 120L).map { today.plusDays(it) }
        val picks = days.map { Challenge.forDate(it) }
        assertEquals(Challenge.entries.toSet(), picks.toSet())
        picks.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        // Isti datum vedno da isti izziv
        assertEquals(Challenge.forDate(today), Challenge.forDate(LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun `challenge evaluation`() {
        val perfect = DailyAggregate(today, 2, 2)
        val partial = DailyAggregate(today, 2, 1)
        assertTrue(Gamification.challengeDone(Challenge.PERFECT_DAY, null, perfect))
        assertFalse(Gamification.challengeDone(Challenge.PERFECT_DAY, null, partial))
        assertTrue(Gamification.challengeDone(Challenge.EARLY_START, DayActivity(today, 1, 1, 1, 0), partial))
        assertFalse(Gamification.challengeDone(Challenge.REFLECT, DayActivity(today, 1, 1, 1, 0), partial))
        assertTrue(Gamification.challengeDone(Challenge.REFLECT, DayActivity(today, 1, 0, 0, 1), partial))
        // Do vecera: vse opravljeno in vse pred 18:00
        assertTrue(Gamification.challengeDone(Challenge.DONE_BY_EVENING, DayActivity(today, 2, 0, 2, 0), perfect))
        assertFalse(Gamification.challengeDone(Challenge.DONE_BY_EVENING, DayActivity(today, 2, 0, 1, 0), perfect))
    }

    @Test
    fun `challenge xp counts each completed day once`() {
        val days = (1L..10L).map { today.minusDays(it) }
        // Vsak dan izpolni vse stiri vrste izzivov
        val state = Gamification.compute(
            input(
                activity = days.map { DayActivity(it, done = 1, doneBefore9 = 1, doneBefore18 = 1, notes = 1) },
                aggregates = days.map { DailyAggregate(it, 1, 1) }
            )
        )
        assertEquals(10, state.stats.challengesDone)
        assertEquals(10 * XpRules.CHALLENGE, state.xp.challenges)
    }

    @Test
    fun `perfect run ignores days without scheduled habits`() {
        val aggregates = listOf(
            DailyAggregate(today.minusDays(9), 1, 0), // zgresen dan
            DailyAggregate(today.minusDays(8), 1, 1),
            DailyAggregate(today.minusDays(7), 1, 1),
            // 6. dan ni povzetka (vikend brez navad)
            DailyAggregate(today.minusDays(5), 1, 1),
            DailyAggregate(today.minusDays(4), 0, 0),
            DailyAggregate(today.minusDays(3), 2, 2),
            DailyAggregate(today.minusDays(2), 2, 1),
            DailyAggregate(today.minusDays(1), 1, 1)
        )
        val state = Gamification.compute(input(aggregates = aggregates))
        assertEquals(4, state.stats.longestPerfectRun)
        assertEquals(5, state.stats.perfectDays)
    }

    @Test
    fun `badges unlock at their targets`() {
        val state = Gamification.compute(
            input(
                activity = listOf(DayActivity(today.minusDays(1), done = 1, doneBefore9 = 0, doneBefore18 = 0, notes = 0)),
                bestStreak = 7
            )
        )
        assertTrue(Badge.FIRST_STEP in state.earned)
        assertTrue(Badge.STREAK_7 in state.earned)
        assertFalse(Badge.STREAK_30 in state.earned)
        assertEquals(7, state.badges.first { it.badge == Badge.STREAK_30 }.current)
        assertFalse(Badge.FIRST_SESSION in state.earned)
    }

    @Test
    fun `future days are ignored`() {
        val tomorrow = today.plusDays(1)
        val state = Gamification.compute(
            input(activity = listOf(DayActivity(tomorrow, 5, 5, 5, 5)), aggregates = listOf(DailyAggregate(tomorrow, 5, 5)))
        )
        assertEquals(0, state.xp.total)
    }

    @Test
    fun `clean days of quit habits earn habit xp`() {
        val state = Gamification.compute(GameInput(emptyList(), emptyList(), emptyList(), 0, today, cleanDays = 4))
        assertEquals(4, state.stats.totalDone)
        assertEquals(40, state.xp.habits)
    }

    @Test
    fun `done by evening ignores clean quit days without a record`() {
        // 3 nacrtovane: 2 vpisani pred 18:00 + 1 cist dan opuscanja (brez vpisa)
        val perfect = DailyAggregate(today, scheduled = 3, done = 3)
        assertTrue(Gamification.challengeDone(Challenge.DONE_BY_EVENING, DayActivity(today, 2, 0, 2, 0), perfect))
    }
}
