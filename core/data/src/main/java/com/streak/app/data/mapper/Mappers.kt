package com.streak.app.data.mapper

import com.streak.app.data.local.entity.CompletionEntity
import com.streak.app.data.local.entity.DailyAggregateEntity
import com.streak.app.data.local.entity.HabitEntity
import com.streak.app.data.local.entity.HabitReminderEntity
import com.streak.app.data.local.entity.MoodEntity
import com.streak.app.data.local.entity.SessionEntity
import com.streak.app.data.local.entity.StreakCacheEntity
import com.streak.app.data.local.entity.TaskEntity
import com.streak.app.data.local.entity.VacationEntity
import com.streak.app.domain.model.Completion
import com.streak.app.domain.model.DailyAggregate
import com.streak.app.domain.model.Habit
import com.streak.app.domain.model.HabitSchedule
import com.streak.app.domain.model.HabitSession
import com.streak.app.domain.model.HabitStreak
import com.streak.app.domain.model.MoodEntry
import com.streak.app.domain.model.Reminder
import com.streak.app.domain.model.Task
import com.streak.app.domain.model.TaskReminder
import com.streak.app.domain.model.Vacation
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// Pretvorbe entitet <-> domenskih modelov

// Bit 0 = ponedeljek ... bit 6 = nedelja
fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

fun weekdaysFromMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

fun HabitEntity.toDomain(reminders: List<HabitReminderEntity>): Habit = Habit(
    id = id,
    title = title,
    description = description,
    icon = icon,
    colorHex = colorHex,
    category = category,
    goalType = goalType,
    targetValue = targetValue,
    unit = unit,
    schedule = HabitSchedule(scheduleType, weekdaysFromMask(weekdaysMask), timesPerPeriod),
    timeOfDay = timeOfDay,
    reminders = reminders.map { it.toDomain() },
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = endEpochDay?.let { LocalDate.ofEpochDay(it) },
    archivedAt = archivedAt,
    createdAt = createdAt,
    sortOrder = sortOrder
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    colorHex = colorHex,
    category = category,
    goalType = goalType,
    targetValue = targetValue,
    unit = unit,
    scheduleType = schedule.type,
    weekdaysMask = schedule.weekdays.toMask(),
    timesPerPeriod = schedule.timesPerPeriod,
    timeOfDay = timeOfDay,
    startEpochDay = startDate.toEpochDay(),
    endEpochDay = endDate?.toEpochDay(),
    archivedAt = archivedAt,
    createdAt = createdAt,
    sortOrder = sortOrder
)

fun Habit.reminderEntities(): List<HabitReminderEntity> = reminders.map {
    HabitReminderEntity(habitId = id, minuteOfDay = it.time.hour * 60 + it.time.minute, enabled = it.enabled)
}

fun HabitReminderEntity.toDomain(): Reminder =
    Reminder(id = id, time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60), enabled = enabled)

fun CompletionEntity.toDomain(): Completion = Completion(
    id = id,
    habitId = habitId,
    date = LocalDate.ofEpochDay(epochDay),
    status = status,
    value = value,
    loggedAt = Instant.ofEpochMilli(loggedAtMillis),
    zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetSeconds),
    note = note,
    manuallyEdited = manuallyEdited
)

fun Completion.toEntity(): CompletionEntity = CompletionEntity(
    id = id,
    habitId = habitId,
    epochDay = date.toEpochDay(),
    status = status,
    value = value,
    loggedAtMillis = loggedAt.toEpochMilli(),
    zoneOffsetSeconds = zoneOffset.totalSeconds,
    note = note,
    manuallyEdited = manuallyEdited
)

fun StreakCacheEntity.toDomain(): HabitStreak =
    HabitStreak(currentStreak, bestStreak, completionRate, unit, totalDone)

fun HabitStreak.toCacheEntity(habitId: String, computedFor: LocalDate): StreakCacheEntity =
    StreakCacheEntity(habitId, currentStreak, bestStreak, completionRate, unit, totalDone, computedFor.toEpochDay())

fun DailyAggregateEntity.toDomain(): DailyAggregate =
    DailyAggregate(LocalDate.ofEpochDay(epochDay), scheduled, done)

fun VacationEntity.toDomain(): Vacation =
    Vacation(id, LocalDate.ofEpochDay(startEpochDay), LocalDate.ofEpochDay(endEpochDay))

fun Vacation.toEntity(): VacationEntity =
    VacationEntity(id, startDate.toEpochDay(), endDate.toEpochDay())

fun SessionEntity.toDomain(): HabitSession = HabitSession(
    id, habitId, LocalDate.ofEpochDay(epochDay),
    Instant.ofEpochMilli(startedAtMillis), Instant.ofEpochMilli(endedAtMillis), durationSeconds
)

fun HabitSession.toEntity(): SessionEntity = SessionEntity(
    id, habitId, date.toEpochDay(), startedAt.toEpochMilli(), endedAt.toEpochMilli(), durationSeconds
)

fun TaskEntity.toDomain(): Task = Task(
    id, title,
    dueEpochDay?.let { LocalDate.ofEpochDay(it) },
    dueMinuteOfDay, priority, isDone, isRecurring, recurrenceRule, createdAt,
    TaskReminder.decode(reminders)
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id, title, dueDate?.toEpochDay(), dueMinuteOfDay, priority,
    isDone, isRecurring, recurrenceRule, createdAt, TaskReminder.encode(reminders)
)

// Neveljavne vrednosti (npr. rocno spremenjena baza) se omejijo na veljaven obseg
fun MoodEntity.toDomain() = MoodEntry(
    date = LocalDate.ofEpochDay(epochDay),
    mood = mood.coerceIn(MoodEntry.RANGE),
    energy = energy?.coerceIn(MoodEntry.RANGE),
    note = note,
    loggedAt = Instant.ofEpochMilli(loggedAtMillis)
)

fun MoodEntry.toEntity() = MoodEntity(
    epochDay = date.toEpochDay(),
    mood = mood,
    energy = energy,
    note = note,
    loggedAtMillis = loggedAt.toEpochMilli()
)
