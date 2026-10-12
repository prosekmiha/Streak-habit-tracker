package com.streak.app.domain.streak

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.Vacation
import java.time.LocalDate

// Izpelje stanje navade za posamezen dan iz urnika, zapisa in dopustov.
object DayEvaluator {

    fun isScheduled(habit: Habit, date: LocalDate): Boolean = when (habit.schedule.type) {
        ScheduleType.DAILY -> true
        ScheduleType.WEEKDAYS -> date.dayOfWeek in habit.schedule.weekdays
        // Pri obdobnih urnikih je vsak dan "mozen", cilj se preverja na obdobje
        ScheduleType.TIMES_PER_WEEK, ScheduleType.TIMES_PER_MONTH -> false
    }

    fun stateOn(
        habit: Habit,
        date: LocalDate,
        completion: Completion?,
        vacations: List<Vacation>,
        today: LocalDate
    ): DayState {
        if (date.isAfter(today)) return DayState.FUTURE
        when (completion?.status) {
            CompletionStatus.DONE -> return DayState.DONE
            CompletionStatus.SKIPPED -> return DayState.SKIPPED
            CompletionStatus.FROZEN -> return DayState.FROZEN
            CompletionStatus.PARTIAL -> return DayState.PARTIAL
            null -> Unit
        }
        if (!habit.isActiveOn(date)) return DayState.INACTIVE
        if (vacations.any { it.contains(date) }) return DayState.VACATION
        val periodic = habit.schedule.type == ScheduleType.TIMES_PER_WEEK ||
            habit.schedule.type == ScheduleType.TIMES_PER_MONTH
        if (periodic) return if (date == today) DayState.PENDING else DayState.NOT_SCHEDULED
        if (!isScheduled(habit, date)) return DayState.NOT_SCHEDULED
        return if (date == today) DayState.PENDING else DayState.MISSED
    }
}
