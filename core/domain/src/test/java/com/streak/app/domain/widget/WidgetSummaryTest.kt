package com.streak.app.domain.widget

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.HabitWithStreak
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.testing.testHabit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class WidgetSummaryTest {

    private val today = LocalDate.of(2026, 10, 11)

    private fun item(habit: Habit, state: DayState, value: Double? = null) = HabitWithStreak(
        habit = habit,
        completions = value?.let {
            mapOf(today to Completion("c-${habit.id}", habit.id, today, CompletionStatus.PARTIAL, it, Instant.EPOCH, ZoneOffset.UTC))
        }.orEmpty(),
        days = mapOf(today to state),
        streak = HabitStreak(3, 5, 0.5f),
        today = today
    )

    @Test
    fun `counts due and done like the home screen`() {
        val s = WidgetSummary.today(
            listOf(
                item(testHabit("a"), DayState.DONE),
                item(testHabit("b"), DayState.PENDING),
                item(testHabit("c"), DayState.PARTIAL, 2.0),
                item(testHabit("d"), DayState.SKIPPED)
            )
        )
        assertEquals(3, s.due)
        assertEquals(1, s.done)
        assertEquals(2, s.remaining)
        assertEquals(WidgetMood.IN_PROGRESS, s.mood)
    }

    @Test
    fun `pending first by time of day, then done, then skipped`() {
        val s = WidgetSummary.today(
            listOf(
                item(testHabit("done"), DayState.DONE),
                item(testHabit("skip"), DayState.SKIPPED),
                item(testHabit("evening").copy(timeOfDay = TimeOfDay.EVENING), DayState.PENDING),
                item(testHabit("morning").copy(timeOfDay = TimeOfDay.MORNING), DayState.PENDING),
                item(testHabit("weekly", type = ScheduleType.TIMES_PER_WEEK, times = 3), DayState.NOT_SCHEDULED)
            )
        )
        assertEquals(listOf("morning", "evening", "weekly", "done", "skip"), s.items.map { it.habit.id })
    }

    @Test
    fun `daily habit outside its schedule is hidden unless logged`() {
        val weekdays = testHabit("w", type = ScheduleType.WEEKDAYS)
        assertEquals(0, WidgetSummary.today(listOf(item(weekdays, DayState.NOT_SCHEDULED))).items.size)
        assertEquals(1, WidgetSummary.today(listOf(item(weekdays, DayState.NOT_SCHEDULED, 1.0))).items.size)
        assertEquals(0, WidgetSummary.today(listOf(item(testHabit("i"), DayState.INACTIVE))).items.size)
    }

    @Test
    fun `mood follows progress`() {
        assertEquals(WidgetMood.NO_HABITS, WidgetSummary.today(emptyList()).mood)
        assertEquals(WidgetMood.REST_DAY, WidgetSummary.today(listOf(item(testHabit("s"), DayState.SKIPPED))).mood)
        assertEquals(WidgetMood.NOT_STARTED, WidgetSummary.today(listOf(item(testHabit("p"), DayState.PENDING))).mood)
        assertEquals(WidgetMood.ALL_DONE, WidgetSummary.today(listOf(item(testHabit("d"), DayState.DONE))).mood)
    }

    @Test
    fun `tap action depends on goal type and state`() {
        fun tap(goal: GoalType, state: DayState) =
            WidgetSummary.habit(item(testHabit("x", goalType = goal, target = 8.0), state)).tap
        assertEquals(WidgetTap.TOGGLE, tap(GoalType.YES_NO, DayState.PENDING))
        assertEquals(WidgetTap.ADD_ONE, tap(GoalType.QUANTITY, DayState.PARTIAL))
        assertEquals(WidgetTap.OPEN_SESSION, tap(GoalType.DURATION, DayState.PENDING))
        // Dotik ne izbrise celega vpisa: opravljena kolicina -1, opravljeno trajanje odpre sejo
        assertEquals(WidgetTap.TOGGLE, tap(GoalType.YES_NO, DayState.DONE))
        assertEquals(WidgetTap.REMOVE_ONE, tap(GoalType.QUANTITY, DayState.DONE))
        assertEquals(WidgetTap.OPEN_SESSION, tap(GoalType.DURATION, DayState.DONE))
        // Zdrs se vpise le v aplikaciji
        assertEquals(WidgetTap.OPEN_APP, tap(GoalType.QUIT, DayState.DONE))
        assertEquals(WidgetTap.OPEN_APP, tap(GoalType.QUIT, DayState.MISSED))
    }

    @Test
    fun `quantity progress is clamped`() {
        val h = testHabit("q", goalType = GoalType.QUANTITY, target = 8.0)
        assertEquals(0.25f, WidgetSummary.habit(item(h, DayState.PARTIAL, 2.0)).progress)
        assertEquals(1f, WidgetSummary.habit(item(h, DayState.DONE, 12.0)).progress)
    }
}
