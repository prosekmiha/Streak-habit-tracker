package com.streak.app.domain.usecase

import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.CompletionStatus.RELAPSE
import com.streak.app.domain.model.CompletionStatus.FROZEN
import com.streak.app.domain.model.CompletionStatus.PARTIAL
import com.streak.app.domain.model.CompletionStatus.SKIPPED
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.Vacation
import com.streak.app.testing.completion
import com.streak.app.testing.completions
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class ComputeStreakUseCaseTest {

    private val compute = ComputeStreakUseCase()
    private val today = LocalDate.of(2026, 10, 7) // sreda
    private fun ago(days: Long) = today.minusDays(days)

    // --- Dnevni urnik ---

    @Test fun `daily - consecutive days including today`() {
        val s = compute(testHabit(), completions(completion(ago(0)), completion(ago(1)), completion(ago(2))), emptyList(), today)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.bestStreak)
        assertEquals(StreakUnit.DAY, s.unit)
        assertEquals(3, s.totalDone)
    }

    @Test fun `daily - today pending does not break`() {
        val s = compute(testHabit(), completions(completion(ago(1)), completion(ago(2))), emptyList(), today)
        assertEquals(2, s.currentStreak)
    }

    @Test fun `daily - day without record breaks the chain (no legacy quirk)`() {
        val s = compute(testHabit(), completions(completion(ago(10)), completion(ago(5))), emptyList(), today)
        assertEquals(0, s.currentStreak)
        assertEquals(1, s.bestStreak)
    }

    @Test fun `daily - skipped and frozen days are bridged`() {
        val c = completions(completion(ago(0)), completion(ago(1), SKIPPED), completion(ago(2)), completion(ago(3), FROZEN), completion(ago(4)))
        val s = compute(testHabit(), c, emptyList(), today)
        assertEquals(3, s.currentStreak)
    }

    @Test fun `daily - vacation bridges the chain`() {
        val vacation = Vacation(startDate = ago(5), endDate = ago(2))
        val c = completions(completion(ago(0)), completion(ago(1)), completion(ago(6)), completion(ago(7)))
        val s = compute(testHabit(), c, listOf(vacation), today)
        assertEquals(4, s.currentStreak)
    }

    @Test fun `daily - partial progress on a past day breaks, today it is pending`() {
        val past = compute(testHabit(), completions(completion(ago(0)), completion(ago(1), PARTIAL), completion(ago(2))), emptyList(), today)
        assertEquals(1, past.currentStreak)
        val todayPartial = compute(testHabit(), completions(completion(ago(0), PARTIAL), completion(ago(1)), completion(ago(2))), emptyList(), today)
        assertEquals(2, todayPartial.currentStreak)
    }

    @Test fun `daily - days before start date are not missed`() {
        val s = compute(testHabit(start = ago(2)), completions(completion(ago(0)), completion(ago(1)), completion(ago(2))), emptyList(), today)
        assertEquals(3, s.currentStreak)
        assertEquals(1f, s.completionRate)
    }

    @Test fun `daily - backfilled completion before start date counts`() {
        val s = compute(testHabit(start = ago(1)), completions(completion(ago(3)), completion(ago(2)), completion(ago(1))), emptyList(), today)
        assertEquals(3, s.currentStreak)
    }

    @Test fun `daily - days after end date are neutral`() {
        val s = compute(testHabit(start = ago(10), end = ago(5)), completions(completion(ago(6)), completion(ago(5))), emptyList(), today)
        assertEquals(2, s.currentStreak)
    }

    @Test fun `daily - completion rate ignores neutral days and pending today`() {
        // zadnjih 30 dni: 3 opravljeni, 1 preskocen, danes se odprt, ostalo zgreseno (26 dni)
        val c = completions(completion(ago(1)), completion(ago(2)), completion(ago(3)), completion(ago(4), SKIPPED))
        val s = compute(testHabit(start = ago(29)), c, emptyList(), today)
        assertEquals(3f / 28f, s.completionRate)
    }

    @Test fun `daily - streak across year boundary`() {
        val newYear = LocalDate.of(2027, 1, 2)
        val c = completions(*(0L..5L).map { completion(newYear.minusDays(it)) }.toTypedArray())
        val s = compute(testHabit(), c, emptyList(), newYear)
        assertEquals(6, s.currentStreak)
    }

    @Test fun `daily - leap day is a normal day`() {
        val day = LocalDate.of(2028, 3, 1)
        val c = completions(completion(day), completion(LocalDate.of(2028, 2, 29)), completion(LocalDate.of(2028, 2, 28)))
        assertEquals(3, compute(testHabit(), c, emptyList(), day).currentStreak)
    }

    // --- Izbrani dnevi v tednu ---

    @Test fun `weekdays - only scheduled days can break the chain`() {
        val habit = testHabit(type = ScheduleType.WEEKDAYS, weekdays = setOf(MONDAY, WEDNESDAY, FRIDAY))
        // sre 7.10. (danes), pon 5.10., pet 2.10., sre 30.9. - vmesni dnevi niso na sporedu
        val c = completions(completion(today), completion(ago(2)), completion(ago(5)), completion(ago(7)))
        val s = compute(habit, c, emptyList(), today)
        assertEquals(4, s.currentStreak)
    }

    @Test fun `weekdays - missed scheduled day breaks`() {
        val habit = testHabit(type = ScheduleType.WEEKDAYS, weekdays = setOf(MONDAY, WEDNESDAY, FRIDAY))
        // pon 5.10. manjka
        val c = completions(completion(today), completion(ago(5)), completion(ago(7)))
        assertEquals(1, compute(habit, c, emptyList(), today).currentStreak)
    }

    @Test fun `weekdays - extra completion on unscheduled day counts`() {
        val habit = testHabit(type = ScheduleType.WEEKDAYS, weekdays = setOf(MONDAY, WEDNESDAY, FRIDAY))
        val c = completions(completion(today), completion(ago(1)), completion(ago(2)))
        assertEquals(3, compute(habit, c, emptyList(), today).currentStreak)
    }

    // --- X-krat na teden / mesec ---

    private fun weekly(times: Int, start: LocalDate = LocalDate.of(2026, 1, 5)) =
        testHabit(type = ScheduleType.TIMES_PER_WEEK, times = times, start = start)

    @Test fun `weekly - satisfied weeks form a chain, current week pending`() {
        // ta teden (od pon 5.10.): 1x; prejsnja dva tedna: 3x
        val c = completions(
            completion(ago(1)),
            completion(ago(7)), completion(ago(8)), completion(ago(9)),
            completion(ago(14)), completion(ago(15)), completion(ago(16))
        )
        val s = compute(weekly(3, start = ago(20)), c, emptyList(), today)
        assertEquals(StreakUnit.WEEK, s.unit)
        assertEquals(2, s.currentStreak)
    }

    @Test fun `weekly - current week counts once satisfied`() {
        val c = completions(completion(ago(0)), completion(ago(1)), completion(ago(7)), completion(ago(8)))
        val s = compute(weekly(2, start = ago(9)), c, emptyList(), today)
        assertEquals(2, s.currentStreak)
    }

    @Test fun `weekly - unsatisfied past week breaks`() {
        val c = completions(completion(ago(0)), completion(ago(1)), completion(ago(8)), completion(ago(14)), completion(ago(15)))
        val s = compute(weekly(2, start = ago(16)), c, emptyList(), today)
        assertEquals(1, s.currentStreak)
        assertEquals(1, s.bestStreak)
    }

    @Test fun `weekly - habit started mid week needs proportionally fewer`() {
        // zacetek v petek 2.10. (3 dni od 7 v tednu): ceil(3 * 3/7) = 2
        val c = completions(completion(LocalDate.of(2026, 10, 3)), completion(LocalDate.of(2026, 10, 4)))
        val s = compute(weekly(3, start = LocalDate.of(2026, 10, 2)), c, emptyList(), today)
        assertEquals(1, s.currentStreak)
    }

    @Test fun `weekly - freeze protects the whole week`() {
        val c = completions(completion(ago(0)), completion(ago(1)), completion(ago(8), FROZEN), completion(ago(14)), completion(ago(15)))
        val s = compute(weekly(2, start = ago(16)), c, emptyList(), today)
        assertEquals(2, s.currentStreak)
    }

    @Test fun `weekly - first day of week setting changes period boundaries`() {
        // Z nedeljo kot prvim dnem: ned 4.10. in pon 5.10. sta v istem tednu
        val c = completions(completion(LocalDate.of(2026, 10, 4)), completion(LocalDate.of(2026, 10, 5)))
        val habit = weekly(2, start = LocalDate.of(2026, 9, 27))
        assertEquals(1, compute(habit, c, emptyList(), today, SUNDAY).currentStreak)
        // S ponedeljkom sta v razlicnih tednih -> prejsnji teden nima 2x
        assertEquals(0, compute(habit, c, emptyList(), today, MONDAY).currentStreak)
    }

    @Test fun `weekly - chain crosses year boundary`() {
        val day = LocalDate.of(2027, 1, 6) // sreda
        val c = completions(
            completion(LocalDate.of(2027, 1, 4)), // pon
            completion(LocalDate.of(2026, 12, 30)), // teden 28.12.-3.1.
            completion(LocalDate.of(2026, 12, 22))
        )
        val s = compute(weekly(1, start = LocalDate.of(2026, 12, 21)), c, emptyList(), day, DayOfWeek.MONDAY)
        assertEquals(3, s.currentStreak)
    }

    @Test fun `monthly - satisfied months`() {
        val habit = testHabit(type = ScheduleType.TIMES_PER_MONTH, times = 2, start = LocalDate.of(2026, 8, 1))
        val c = completions(
            completion(LocalDate.of(2026, 10, 1)), completion(LocalDate.of(2026, 10, 2)),
            completion(LocalDate.of(2026, 9, 10)), completion(LocalDate.of(2026, 9, 20)),
            completion(LocalDate.of(2026, 8, 31))
        )
        val s = compute(habit, c, emptyList(), today)
        assertEquals(StreakUnit.MONTH, s.unit)
        assertEquals(2, s.currentStreak) // avgust ima le 1x -> veriga se zacne septembra
        assertEquals(2, s.bestStreak)
    }

    @Test fun `no completions and future start date gives zero`() {
        val s = compute(testHabit(start = today.plusDays(3)), emptyMap(), emptyList(), today)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.bestStreak)
    }

    // --- Opuscanje ---

    private fun quit(start: LocalDate = ago(9)) = testHabit(goalType = GoalType.QUIT, target = 0.0, start = start)

    @Test fun `quit - every day without a slip is clean, including today`() {
        val s = compute(quit(), emptyMap(), emptyList(), today)
        assertEquals(10, s.currentStreak)
        assertEquals(10, s.bestStreak)
        assertEquals(10, s.totalDone)
        assertEquals(1f, s.completionRate)
    }

    @Test fun `quit - a slip resets the run, best is the longest gap`() {
        val s = compute(quit(), completions(completion(ago(3), RELAPSE, value = 2.0)), emptyList(), today)
        assertEquals(3, s.currentStreak) // ago(2), ago(1), danes
        assertEquals(6, s.bestStreak) // ago(9)..ago(4)
        assertEquals(9, s.totalDone)
    }

    @Test fun `quit - slip today breaks the run immediately`() {
        val s = compute(quit(), completions(completion(ago(0), RELAPSE, value = 1.0)), emptyList(), today)
        assertEquals(0, s.currentStreak)
        assertEquals(9, s.bestStreak)
    }

    @Test fun `quit - future start gives zero`() {
        val s = compute(quit(start = today.plusDays(1)), emptyMap(), emptyList(), today)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.totalDone)
    }
}
