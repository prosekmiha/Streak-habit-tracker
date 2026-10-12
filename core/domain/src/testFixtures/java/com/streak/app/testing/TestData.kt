package com.streak.app.testing

import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSchedule
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.time.DayClock
import com.streak.app.domain.time.LogicalDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

fun testHabit(
    id: String = "h",
    start: LocalDate = LocalDate.of(2020, 1, 1),
    end: LocalDate? = null,
    type: ScheduleType = ScheduleType.DAILY,
    weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    times: Int = 1,
    goalType: GoalType = GoalType.YES_NO,
    target: Double = 1.0
) = Habit(
    id = id,
    title = "Habit $id",
    icon = "Star",
    colorHex = "#000000",
    goalType = goalType,
    targetValue = target,
    schedule = HabitSchedule(type, weekdays, times),
    startDate = start,
    endDate = end,
    createdAt = 0L
)

fun completion(
    date: LocalDate,
    status: CompletionStatus = CompletionStatus.DONE,
    habitId: String = "h",
    value: Double = if (status == CompletionStatus.DONE) 1.0 else 0.0
) = Completion(
    id = "$habitId-$date",
    habitId = habitId,
    date = date,
    status = status,
    value = value,
    loggedAt = Instant.EPOCH,
    zoneOffset = ZoneOffset.UTC
)

fun completions(vararg items: Completion): Map<LocalDate, Completion> = items.associateBy { it.date }

// Ura za teste: rocno nastavljiv trenutek, pas in "dan se konca ob"
class FakeDayClock(
    var instant: Instant = Instant.parse("2026-10-07T10:00:00Z"),
    var zoneId: ZoneId = ZoneId.of("Europe/Ljubljana"),
    var dayEndsAt: LocalTime = LocalTime.MIDNIGHT
) : DayClock {
    private val state = MutableStateFlow(LogicalDay.of(instant, zoneId, dayEndsAt))

    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
    override suspend fun today(): LocalDate = LogicalDay.of(instant, zoneId, dayEndsAt)
    override suspend fun dateOf(instant: Instant): LocalDate = LogicalDay.of(instant, zoneId, dayEndsAt)
    override val todayFlow: Flow<LocalDate> get() = state

    fun set(instant: Instant) {
        this.instant = instant
        state.value = LogicalDay.of(instant, zoneId, dayEndsAt)
    }
}
