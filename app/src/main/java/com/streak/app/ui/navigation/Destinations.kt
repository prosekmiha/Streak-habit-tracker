package com.streak.app.ui.navigation

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

    val TopLevel = setOf(HOME, TASKS, STATS, PROFILE)

    fun addHabit(templateId: String? = null) = if (templateId == null) "add_habit" else "add_habit?template=$templateId"
    fun editHabit(id: String) = "edit_habit/$id"
    fun habitDetail(id: String) = "habit/$id"
    fun session(habitId: String) = "session/$habitId"
    fun editTask(id: String) = "edit_task/$id"
}
