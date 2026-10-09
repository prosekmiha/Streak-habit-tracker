package com.streak.app.ui.onboarding

import androidx.annotation.StringRes
import com.streak.app.R
import com.streak.app.domain.model.GoalType
import com.streak.app.domain.model.HabitCategory
import com.streak.app.domain.model.HabitSchedule
import com.streak.app.domain.model.ScheduleType
import com.streak.app.domain.model.TimeOfDay

// Pripravljene predloge navad. Besedila so lokalizirana ob ustvarjanju navade.
data class HabitTemplate(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val icon: String,
    val colorHex: String,
    val category: HabitCategory,
    val goalType: GoalType = GoalType.YES_NO,
    val target: Double = 1.0,
    @StringRes val unit: Int? = null,
    val schedule: HabitSchedule = HabitSchedule.Daily,
    val timeOfDay: TimeOfDay = TimeOfDay.ANYTIME
)

object HabitTemplates {
    val all = listOf(
        HabitTemplate("reading", R.string.tpl_reading, R.string.tpl_reading_desc, "Book", "#F7CD63", HabitCategory.LEARNING,
            GoalType.QUANTITY, 20.0, R.string.unit_pages, timeOfDay = TimeOfDay.EVENING),
        HabitTemplate("household", R.string.tpl_household, R.string.tpl_household_desc, "Iron", "#B8EB6C", HabitCategory.HOME,
            timeOfDay = TimeOfDay.MORNING),
        HabitTemplate("water", R.string.tpl_water, R.string.tpl_water_desc, "Water", "#A9C8F5", HabitCategory.HEALTH,
            GoalType.QUANTITY, 8.0, R.string.unit_glasses),
        HabitTemplate("meditation", R.string.tpl_meditation, R.string.tpl_meditation_desc, "Meditation", "#A6E3D8", HabitCategory.MINDFULNESS,
            GoalType.DURATION, 10 * 60.0, timeOfDay = TimeOfDay.MORNING),
        HabitTemplate("positive", R.string.tpl_positive, R.string.tpl_positive_desc, "Celebration", "#C8BDF7", HabitCategory.MINDFULNESS),
        HabitTemplate("finance", R.string.tpl_finance, R.string.tpl_finance_desc, "Wallet", "#FC8FC6", HabitCategory.FINANCE,
            timeOfDay = TimeOfDay.EVENING),
        HabitTemplate("exercise", R.string.tpl_exercise, R.string.tpl_exercise_desc, "Fitness", "#FFB38A", HabitCategory.HEALTH,
            schedule = HabitSchedule(ScheduleType.TIMES_PER_WEEK, timesPerPeriod = 3)),
        HabitTemplate("coding", R.string.tpl_coding, R.string.tpl_coding_desc, "Code", "#A9C8F5", HabitCategory.LEARNING),
        HabitTemplate("sleep", R.string.tpl_sleep, R.string.tpl_sleep_desc, "Sleep", "#C8BDF7", HabitCategory.HEALTH,
            timeOfDay = TimeOfDay.EVENING)
    )
}
