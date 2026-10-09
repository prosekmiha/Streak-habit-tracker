package com.streak.app.ui.navigation

// Cilji navigacije
object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val ADD_HABIT = "add_habit"
    const val EDIT_HABIT = "edit_habit/{habitId}"
    const val ADD_TASK = "add_task"
    const val EDIT_TASK = "edit_task/{taskId}"

    val TopLevel = setOf(HOME, TASKS, STATS, PROFILE)

    fun editHabit(id: String) = "edit_habit/$id"
    fun editTask(id: String) = "edit_task/$id"
}
