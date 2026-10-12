package com.streak.app.ui.navigation

import android.net.Uri

// Cilji navigacije
object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val ADD_HABIT = "add_habit?template={template}"
    const val EDIT_HABIT = "edit_habit/{habitId}"
    const val HABIT_DETAIL = "habit/{habitId}"
    const val SESSION = "session/{habitId}"
    const val ADD_TASK = "add_task"
    const val EDIT_TASK = "edit_task/{taskId}"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val DATA = "data"
    const val ACHIEVEMENTS = "achievements"
    const val REVIEW = "review"
    // Ni zaslon: odpre list za razpolozenje (obvestilo)
    const val MOOD = "mood"

    val TopLevel = setOf(HOME, TASKS, STATS, PROFILE)

    // ID-ji so kodirani: ID iz tuje namere (npr. z "/") ne sme pokvariti poti in sesuti navigacije
    fun addHabit(templateId: String? = null) = if (templateId == null) "add_habit" else "add_habit?template=${Uri.encode(templateId)}"
    fun editHabit(id: String) = "edit_habit/${Uri.encode(id)}"
    fun habitDetail(id: String) = "habit/${Uri.encode(id)}"
    fun session(habitId: String) = "session/${Uri.encode(habitId)}"
    fun editTask(id: String) = "edit_task/${Uri.encode(id)}"
}
