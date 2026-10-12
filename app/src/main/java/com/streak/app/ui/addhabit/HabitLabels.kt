package com.streak.app.ui.addhabit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay

// Lokalizirane oznake lastnosti navade

@Composable
internal fun scheduleLabel(type: ScheduleType) = stringResource(
    when (type) {
        ScheduleType.DAILY -> R.string.interval_daily
        ScheduleType.WEEKDAYS -> R.string.interval_weekdays
        ScheduleType.TIMES_PER_WEEK -> R.string.interval_times_week
        ScheduleType.TIMES_PER_MONTH -> R.string.interval_times_month
    }
)

@Composable
internal fun scheduleDescription(type: ScheduleType) = stringResource(
    when (type) {
        ScheduleType.DAILY -> R.string.interval_daily_desc
        ScheduleType.WEEKDAYS -> R.string.interval_weekdays_desc
        ScheduleType.TIMES_PER_WEEK -> R.string.interval_times_week_desc
        ScheduleType.TIMES_PER_MONTH -> R.string.interval_times_month_desc
    }
)

@Composable
internal fun goalLabel(type: GoalType) = stringResource(
    when (type) {
        GoalType.YES_NO -> R.string.goal_yes_no
        GoalType.QUANTITY -> R.string.goal_quantity
        GoalType.DURATION -> R.string.goal_duration
    }
)

@Composable
fun categoryLabel(c: HabitCategory) = stringResource(
    when (c) {
        HabitCategory.NONE -> R.string.category_none
        HabitCategory.HEALTH -> R.string.category_health
        HabitCategory.FINANCE -> R.string.category_finance
        HabitCategory.LEARNING -> R.string.category_learning
        HabitCategory.HOME -> R.string.category_home
        HabitCategory.MINDFULNESS -> R.string.category_mindfulness
        HabitCategory.WORK -> R.string.category_work
    }
)

@Composable
fun timeOfDayLabel(t: TimeOfDay) = stringResource(
    when (t) {
        TimeOfDay.ANYTIME -> R.string.time_of_day_anytime
        TimeOfDay.MORNING -> R.string.filter_morning
        TimeOfDay.AFTERNOON -> R.string.filter_afternoon
        TimeOfDay.EVENING -> R.string.filter_evening
    }
)
