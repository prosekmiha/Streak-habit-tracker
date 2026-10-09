package com.streak.app.data.local

import androidx.room.TypeConverter
import com.streak.app.domain.model.CompletionStatus
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.Priority
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.StreakUnit
import com.streak.app.domain.model.TimeOfDay

// Pretvorbe enumov v/iz String za Room (ime, ne ordinal - varno ob dodajanju vrednosti)
class Converters {
    @TypeConverter fun goalToString(v: GoalType): String = v.name
    @TypeConverter fun stringToGoal(v: String): GoalType = GoalType.valueOf(v)

    @TypeConverter fun scheduleToString(v: ScheduleType): String = v.name
    @TypeConverter fun stringToSchedule(v: String): ScheduleType = ScheduleType.valueOf(v)

    @TypeConverter fun timeOfDayToString(v: TimeOfDay): String = v.name
    @TypeConverter fun stringToTimeOfDay(v: String): TimeOfDay = TimeOfDay.valueOf(v)

    @TypeConverter fun categoryToString(v: HabitCategory): String = v.name
    @TypeConverter fun stringToCategory(v: String): HabitCategory = HabitCategory.valueOf(v)

    @TypeConverter fun statusToString(v: CompletionStatus): String = v.name
    @TypeConverter fun stringToStatus(v: String): CompletionStatus = CompletionStatus.valueOf(v)

    @TypeConverter fun unitToString(v: StreakUnit): String = v.name
    @TypeConverter fun stringToUnit(v: String): StreakUnit = StreakUnit.valueOf(v)

    @TypeConverter fun priorityToString(v: Priority): String = v.name
    @TypeConverter fun stringToPriority(v: String): Priority = Priority.valueOf(v)
}
