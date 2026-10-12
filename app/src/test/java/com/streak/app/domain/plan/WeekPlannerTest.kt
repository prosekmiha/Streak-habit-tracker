package com.streak.app.domain.plan

import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.Vacation
import com.streak.app.testing.completion
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekPlannerTest {

    // Teden pon 5. 10. – ned 11. 10. 2026, danes je sreda
    private val monday = LocalDate.of(2026, 10, 5)
    private val today = monday.plusDays(2)

    @Test fun dailyHabitIsMissedBeforeTodayAndPlannedAfter() {
        val habit = testHabit()
        val plan = WeekPlanner.build(listOf(habit), monday, listOf(completion(monday)), emptyList(), today)
        val row = plan.groups.single().rows.single()
        assertEquals(
            listOf(PlanCell.DONE, PlanCell.MISSED, PlanCell.PLANNED, PlanCell.PLANNED, PlanCell.PLANNED, PlanCell.PLANNED, PlanCell.PLANNED),
            row.cells
        )
        assertEquals(1, row.done)
        assertEquals(7, row.target)
    }

    @Test fun weekdayHabitIsOffOnOtherDays() {
        val habit = testHabit(type = ScheduleType.WEEKDAYS, weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val plan = WeekPlanner.build(listOf(habit), monday, emptyList(), emptyList(), today)
        val row = plan.groups.single().rows.single()
        assertEquals(PlanCell.MISSED, row.cells[0])
        assertEquals(PlanCell.OFF, row.cells[1])
        assertEquals(PlanCell.PLANNED, row.cells[4])
        assertEquals(2, row.target)
        assertEquals(listOf(1, 0, 0, 0, 1, 0, 0), plan.plannedPerDay)
    }

    @Test fun timesPerWeekCountsDoneInWeekAndIsFlexible() {
        val habit = testHabit(type = ScheduleType.TIMES_PER_WEEK, times = 3)
        val logs = listOf(completion(monday), completion(monday.plusDays(1)), completion(monday.minusDays(1)))
        val plan = WeekPlanner.build(listOf(habit), monday, logs, emptyList(), today)
        val row = plan.groups.single().rows.single()
        assertEquals(2, row.done)
        assertEquals(3, row.target)
        assertEquals(PlanCell.FLEXIBLE, row.cells[2])
        // Obdobne navade ne stejejo v dnevni stevec
        assertTrue(plan.plannedPerDay.all { it == 0 })
    }

    @Test fun timesPerMonthCountsWholeMonth() {
        val habit = testHabit(type = ScheduleType.TIMES_PER_MONTH, times = 4)
        val logs = listOf(completion(LocalDate.of(2026, 10, 1)), completion(monday), completion(LocalDate.of(2026, 9, 30)))
        val row = WeekPlanner.build(listOf(habit), monday, logs, emptyList(), today).groups.single().rows.single()
        assertEquals(2, row.done)
        assertEquals(4, row.target)
    }

    @Test fun vacationSkipsAndInactiveHabitsAreHidden() {
        val ended = testHabit(id = "old", end = monday.minusDays(1))
        val habit = testHabit(id = "h")
        val vacation = Vacation(startDate = monday.plusDays(5), endDate = monday.plusDays(6))
        val logs = listOf(completion(monday.plusDays(1), CompletionStatus.SKIPPED))
        val plan = WeekPlanner.build(listOf(ended, habit), monday, logs, listOf(vacation), today)
        val row = plan.groups.single().rows.single()
        assertEquals("h", row.habit.id)
        assertEquals(PlanCell.SKIPPED, row.cells[1])
        assertEquals(PlanCell.SKIPPED, row.cells[5])
        assertEquals(PlanCell.SKIPPED, row.cells[6])
    }

    @Test fun groupsFollowScheduleOrder() {
        val habits = listOf(
            testHabit(id = "m", type = ScheduleType.TIMES_PER_MONTH),
            testHabit(id = "d"),
            testHabit(id = "w", type = ScheduleType.TIMES_PER_WEEK)
        )
        val plan = WeekPlanner.build(habits, monday, emptyList(), emptyList(), today)
        assertEquals(listOf(ScheduleType.DAILY, ScheduleType.TIMES_PER_WEEK, ScheduleType.TIMES_PER_MONTH), plan.groups.map { it.type })
    }
}
