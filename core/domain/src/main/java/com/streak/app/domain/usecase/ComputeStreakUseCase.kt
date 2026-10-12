package com.streak.app.domain.usecase

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.Vacation
import com.streak.app.domain.streak.DayEvaluator
import com.streak.app.domain.streak.PeriodGoal
import com.streak.app.domain.streak.Periods
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

// Izracun streaka.
//
// Dnevni urniki (DAILY, WEEKDAYS) - enota je dan:
// - DONE poveca verigo (tudi na dan, ki ni na sporedu)
// - SKIPPED, FROZEN, dopust, dnevi izven urnika in izven trajanja navade so nevtralni
// - nacrtovan pretekli dan brez DONE (tudi PARTIAL) prekine verigo
// - danasnji dan se ne steje kot zgresen, dokler se ne konca
//
// Obdobni urniki (TIMES_PER_WEEK / TIMES_PER_MONTH) - enota je teden/mesec:
// - obdobje je izpolnjeno, ce je stevilo DONE dni >= zahtevano
// - zahtevano stevilo se sorazmerno zmanjsa za nevtralne dni (preskok, dopust,
//   dnevi pred zacetkom ali po koncu navade); zamrznitev zascitit celo obdobje
// - tekoce obdobje prekine verigo sele, ko se konca
//
// Opuscanje (QUIT) - enota je dan:
// - vsak aktiven dan brez zdrsa (tudi danes) je cist in poveca verigo, zdrs jo prekine
// - totalDone je stevilo cistih dni (zapisa za ciste dni ni)
class ComputeStreakUseCase @Inject constructor() {

    operator fun invoke(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
    ): HabitStreak {
        if (habit.isQuit) return quitStreak(habit, completions, vacations, today)
        val totalDone = completions.values.count { it.status == CompletionStatus.DONE }
        val firstCompletion = completions.keys.minOrNull()
        val effectiveStart = listOfNotNull(habit.startDate, firstCompletion).min()
        if (effectiveStart.isAfter(today)) {
            return HabitStreak(0, 0, 0f, habit.schedule.streakUnit, totalDone)
        }
        return when (val unit = habit.schedule.streakUnit) {
            StreakUnit.DAY -> dailyStreak(habit, completions, vacations, today, effectiveStart, totalDone)
            StreakUnit.WEEK, StreakUnit.MONTH ->
                periodStreak(habit, completions, vacations, today, effectiveStart, unit, firstDayOfWeek, totalDone)
        }
    }

    private fun dailyStreak(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        today: LocalDate,
        start: LocalDate,
        totalDone: Int
    ): HabitStreak {
        var run = 0
        var best = 0
        var day = start
        while (!day.isAfter(today)) {
            when (DayEvaluator.stateOn(habit, day, completions[day], vacations, today)) {
                DayState.DONE -> { run++; if (run > best) best = run }
                DayState.MISSED -> run = 0
                DayState.PARTIAL -> if (day != today) run = 0
                else -> Unit // nevtralno
            }
            day = day.plusDays(1)
        }

        // Delez za zadnjih 30 dni: opravljeni / (opravljeni + zgreseni); danes le, ce je opravljen
        val windowStart = maxOf(start, today.minusDays(29))
        var done = 0
        var counted = 0
        day = windowStart
        while (!day.isAfter(today)) {
            when (DayEvaluator.stateOn(habit, day, completions[day], vacations, today)) {
                DayState.DONE -> { done++; counted++ }
                DayState.MISSED -> counted++
                DayState.PARTIAL -> if (day != today) counted++
                else -> Unit
            }
            day = day.plusDays(1)
        }
        val rate = if (counted == 0) 0f else done.toFloat() / counted
        return HabitStreak(run, best, rate, StreakUnit.DAY, totalDone)
    }

    private fun quitStreak(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        today: LocalDate
    ): HabitStreak {
        if (habit.startDate.isAfter(today)) return HabitStreak(0, 0, 0f, StreakUnit.DAY, 0)
        var run = 0
        var best = 0
        var clean = 0
        var day = habit.startDate
        while (!day.isAfter(today)) {
            when (DayEvaluator.stateOn(habit, day, completions[day], vacations, today)) {
                DayState.DONE -> { run++; clean++; if (run > best) best = run }
                DayState.MISSED -> run = 0
                else -> Unit // preskok, zamrznitev, izven trajanja
            }
            day = day.plusDays(1)
        }
        val windowStart = maxOf(habit.startDate, today.minusDays(29))
        var done = 0
        var counted = 0
        day = windowStart
        while (!day.isAfter(today)) {
            when (DayEvaluator.stateOn(habit, day, completions[day], vacations, today)) {
                DayState.DONE -> { done++; counted++ }
                DayState.MISSED -> counted++
                else -> Unit
            }
            day = day.plusDays(1)
        }
        val rate = if (counted == 0) 0f else done.toFloat() / counted
        return HabitStreak(run, best, rate, StreakUnit.DAY, clean)
    }

    private fun periodStreak(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        today: LocalDate,
        start: LocalDate,
        unit: StreakUnit,
        firstDayOfWeek: DayOfWeek,
        totalDone: Int
    ): HabitStreak {
        val currentPeriod = Periods.start(today, unit, firstDayOfWeek)
        var period = Periods.start(start, unit, firstDayOfWeek)
        var run = 0
        var best = 0
        // (opravljeno, zahtevano) za zakljucena obdobja - za delez
        val finished = ArrayList<Pair<Int, Int>>()
        var current: PeriodGoal.Result? = null

        while (!period.isAfter(currentPeriod)) {
            val result = PeriodGoal.evaluate(habit, completions, vacations, period, unit, start)
            val isCurrent = period == currentPeriod
            when {
                result.satisfied -> { run++; if (run > best) best = run }
                result.required == 0 -> Unit // nevtralno obdobje
                isCurrent -> Unit // se tece
                else -> run = 0
            }
            if (isCurrent) current = result
            else if (result.required > 0) finished += result.done.coerceAtMost(result.required) to result.required
            period = Periods.next(period, unit)
        }

        val last = finished.takeLast(4)
        val rate = when {
            last.isNotEmpty() -> last.sumOf { it.first }.toFloat() / last.sumOf { it.second }
            current != null && current.required > 0 ->
                current.done.coerceAtMost(current.required).toFloat() / current.required
            else -> 0f
        }
        return HabitStreak(run, best, rate.coerceIn(0f, 1f), unit, totalDone)
    }
}
