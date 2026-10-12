package com.streak.app.domain.streak

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.Vacation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

// Cilj obdobja pri navadah "n-krat na teden/mesec"
object PeriodGoal {

    class Result(val done: Int, val required: Int) {
        val satisfied: Boolean get() = done > 0 && done >= required
    }

    // Opravljeni dnevi in zahtevano stevilo za obdobje, ki se zacne s periodStart.
    // Zahtevano se sorazmerno zmanjsa za nevtralne dni (preskok, dopust, dnevi pred
    // zacetkom ali po koncu navade); zamrznitev zascitit celo obdobje (zahtevano 0).
    fun evaluate(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        periodStart: LocalDate,
        unit: StreakUnit,
        effectiveStart: LocalDate
    ): Result {
        val periodEnd = Periods.end(periodStart, unit)
        val length = ChronoUnit.DAYS.between(periodStart, periodEnd).toInt() + 1
        var done = 0
        var neutral = 0
        var frozen = false
        var day = periodStart
        while (!day.isAfter(periodEnd)) {
            val c = completions[day]
            when {
                c?.status == CompletionStatus.DONE -> done++
                c?.status == CompletionStatus.FROZEN -> frozen = true
                c?.status == CompletionStatus.SKIPPED -> neutral++
                day.isBefore(effectiveStart) -> neutral++
                habit.endDate != null && day.isAfter(habit.endDate) -> neutral++
                vacations.any { it.contains(day) } -> neutral++
            }
            day = day.plusDays(1)
        }
        if (frozen) return Result(done, required = 0)
        val activeDays = length - neutral
        // Nikoli vec, kot je aktivnih dni (npr. "30-krat na mesec" v februarju)
        val required = ceil(habit.schedule.timesPerPeriod.toDouble() * activeDays / length).toInt().coerceAtMost(activeDays)
        return Result(done, required)
    }

    // Ali je navada "n-krat na obdobje" v obdobju, ki vsebuje "date", ze izpolnila cilj
    // (takrat danes ni vec nujna: brez opomnika, ne steje med neopravljene)
    fun isMet(
        habit: Habit,
        completions: Map<LocalDate, Completion>,
        vacations: List<Vacation>,
        date: LocalDate,
        firstDayOfWeek: DayOfWeek
    ): Boolean {
        val unit = when (habit.schedule.type) {
            ScheduleType.TIMES_PER_WEEK -> StreakUnit.WEEK
            ScheduleType.TIMES_PER_MONTH -> StreakUnit.MONTH
            else -> return false
        }
        val effectiveStart = listOfNotNull(habit.startDate, completions.keys.minOrNull()).min()
        return evaluate(habit, completions, vacations, Periods.start(date, unit, firstDayOfWeek), unit, effectiveStart).satisfied
    }
}
