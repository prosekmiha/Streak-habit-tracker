package com.streak.app.ui.navigation

// Cilji navigacije
object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ADD_HABIT = "add_habit"
    const val EDIT_HABIT = "edit_habit/{habitId}"
    const val ADD_TASK = "add_task"
    const val EDIT_TASK = "edit_task/{taskId}"

    fun editHabit(id: String) = "edit_habit/$id"
    fun editTask(id: String) = "edit_task/$id"
}
