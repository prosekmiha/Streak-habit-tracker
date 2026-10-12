package com.streak.app.ui.home

import com.streak.app.domain.model.DayState
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.TimeOfDay
import com.streak.app.testing.testHabit
import com.streak.app.testing.withStreak
import com.streak.app.ui.components.WeekDayStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HomeUiMappingTest {

    private val today = LocalDate.of(2026, 10, 7)

    private fun card(id: String, timeOfDay: TimeOfDay) =
        HomeUiMapping.card(withStreak(testHabit(id).copy(timeOfDay = timeOfDay), today), today)!!

    @Test
    fun `selected date outside the strip falls back to today`() {
        assertEquals(today, HomeUiMapping.clampDate(null, today))
        assertEquals(today.minusDays(3), HomeUiMapping.clampDate(today.minusDays(3), today))
        assertEquals(today, HomeUiMapping.clampDate(today.minusDays(HomeUiMapping.PAST_DAYS + 1), today))
        assertEquals(today, HomeUiMapping.clampDate(today.plusDays(HomeUiMapping.FUTURE_DAYS + 1), today))
    }

    @Test
    fun `daily habit that is not scheduled and has no record is hidden`() {
        val habit = withStreak(testHabit("a"), today, days = mapOf(today to DayState.NOT_SCHEDULED))
        assertNull(HomeUiMapping.card(habit, today))
    }

    @Test
    fun `weekly habit that is not scheduled today is still shown`() {
        val habit = withStreak(testHabit("a", type = ScheduleType.TIMES_PER_WEEK, times = 3), today, days = mapOf(today to DayState.NOT_SCHEDULED))
        assertNotNull(HomeUiMapping.card(habit, today))
    }

    @Test
    fun `sections get titles only when every group has at least two habits`() {
        val grouped = HomeUiMapping.sections(
            listOf(card("a", TimeOfDay.EVENING), card("b", TimeOfDay.MORNING), card("c", TimeOfDay.MORNING), card("d", TimeOfDay.EVENING))
        )
        assertEquals(listOf(TimeOfDay.MORNING, TimeOfDay.EVENING), grouped.map { it.timeOfDay })

        val flat = HomeUiMapping.sections(listOf(card("a", TimeOfDay.EVENING), card("b", TimeOfDay.MORNING), card("c", TimeOfDay.MORNING)))
        assertEquals(1, flat.size)
        assertNull(flat.single().timeOfDay)
        // Vrstni red sledi delu dneva tudi brez naslovov
        assertEquals(listOf("b", "c", "a"), flat.single().items.map { it.habit.id })
    }

    @Test
    fun `week strip marks days by completion`() {
        val yesterday = today.minusDays(1)
        val habits = listOf(
            withStreak(testHabit("a"), today, days = mapOf(today to DayState.DONE, yesterday to DayState.DONE)),
            withStreak(testHabit("b"), today, days = mapOf(today to DayState.PENDING, yesterday to DayState.DONE))
        )
        val days = HomeUiMapping.weekDays(habits, today).associateBy { it.date }

        assertEquals(WeekDayStatus.ALL_DONE, days.getValue(yesterday).status)
        assertEquals(WeekDayStatus.SOME_DONE, days.getValue(today).status)
        assertEquals(WeekDayStatus.FUTURE, days.getValue(today.plusDays(1)).status)
        assertEquals((HomeUiMapping.PAST_DAYS + HomeUiMapping.FUTURE_DAYS + 1).toInt(), days.size)
    }

    @Test
    fun `top streak counts only daily habits`() {
        val habits = listOf(
            withStreak(testHabit("a"), today, streak = HabitStreak(5, 5, 1f)),
            withStreak(testHabit("b", type = ScheduleType.TIMES_PER_WEEK), today, streak = HabitStreak(9, 9, 1f, StreakUnit.WEEK))
        )
        assertEquals(5, HomeUiMapping.topDailyStreak(habits))
    }
}
