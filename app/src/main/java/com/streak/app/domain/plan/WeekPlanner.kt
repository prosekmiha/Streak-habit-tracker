package com.streak.app.domain.plan

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.Vacation
import com.streak.app.domain.streak.DayEvaluator
import java.time.LocalDate
import java.time.YearMonth

// Stanje navade na dan v tedenskem planu
enum class PlanCell {
    PLANNED,  // na urniku, dan se ni minil
    DONE,
    PARTIAL,
    MISSED,   // na urniku, dan je minil brez cilja
    SKIPPED,  // preskok, zamrznitev ali dopust
    FLEXIBLE, // obdobna navada: mozna kateri koli dan
    OFF,      // ni na urniku
    INACTIVE  // pred zacetkom ali po koncu navade
}

// Vrstica plana: stanja za 7 dni in napredek (opravljeno / cilj v obdobju)
data class PlanRow(val habit: Habit, val cells: List<PlanCell>, val done: Int, val target: Int)

data class PlanGroup(val type: ScheduleType, val rows: List<PlanRow>)

data class WeekPlan(
    val days: List<LocalDate>,
    val groups: List<PlanGroup>,
    // Stevilo navad, vezanih na dan (dnevne in izbrani dnevi), za glavo stolpca
    val plannedPerDay: List<Int>
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

// Sestavi tedenski plan iz urnikov, zapisov in dopustov.
object WeekPlanner {

    fun build(
        habits: List<Habit>,
        weekStart: LocalDate,
        completions: List<Completion>,
        vacations: List<Vacation>,
        today: LocalDate
    ): WeekPlan {
        val days = (0L..6L).map { weekStart.plusDays(it) }
        val weekEnd = days.last()
        // Mesec za navade "X-krat na mesec": tekoci, ce je danes v tem tednu, sicer mesec zacetka tedna
        val month = YearMonth.from(if (!today.isBefore(weekStart) && !today.isAfter(weekEnd)) today else weekStart)
        val byHabit = completions.groupBy { it.habitId }

        val rows = habits.mapNotNull { habit ->
            val own = byHabit[habit.id].orEmpty()
            val byDate = own.associateBy { it.date }
            val visible = days.any { habit.isActiveOn(it) || byDate.containsKey(it) }
            if (!visible) return@mapNotNull null
            val cells = days.map { cellOn(habit, it, byDate[it], vacations, today) }
            val (done, target) = when (habit.schedule.type) {
                ScheduleType.TIMES_PER_WEEK ->
                    own.count { it.status == CompletionStatus.DONE && !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) } to
                        habit.schedule.timesPerPeriod
                ScheduleType.TIMES_PER_MONTH ->
                    own.count { it.status == CompletionStatus.DONE && YearMonth.from(it.date) == month } to
                        habit.schedule.timesPerPeriod
                ScheduleType.DAILY, ScheduleType.WEEKDAYS ->
                    cells.count { it == PlanCell.DONE } to cells.count { it.isPlanned() }
            }
            PlanRow(habit, cells, done, target)
        }

        val groups = ScheduleType.entries.mapNotNull { type ->
            rows.filter { it.habit.schedule.type == type }.takeIf { it.isNotEmpty() }?.let { PlanGroup(type, it) }
        }
        val plannedPerDay = days.indices.map { i ->
            rows.count { !it.habit.isPeriodic() && it.cells[i].isPlanned() }
        }
        return WeekPlan(days, groups, plannedPerDay)
    }

    fun cellOn(habit: Habit, date: LocalDate, completion: Completion?, vacations: List<Vacation>, today: LocalDate): PlanCell {
        when (completion?.status) {
            CompletionStatus.DONE -> return PlanCell.DONE
            CompletionStatus.PARTIAL -> return PlanCell.PARTIAL
            CompletionStatus.SKIPPED, CompletionStatus.FROZEN -> return PlanCell.SKIPPED
            null -> Unit
        }
        if (!habit.isActiveOn(date)) return PlanCell.INACTIVE
        if (habit.isPeriodic()) return PlanCell.FLEXIBLE
        if (!DayEvaluator.isScheduled(habit, date)) return PlanCell.OFF
        if (vacations.any { it.contains(date) }) return PlanCell.SKIPPED
        return if (date.isBefore(today)) PlanCell.MISSED else PlanCell.PLANNED
    }

    private fun PlanCell.isPlanned() = this != PlanCell.OFF && this != PlanCell.INACTIVE && this != PlanCell.FLEXIBLE

    private fun Habit.isPeriodic() =
        schedule.type == ScheduleType.TIMES_PER_WEEK || schedule.type == ScheduleType.TIMES_PER_MONTH
}
