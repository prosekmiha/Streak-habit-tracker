package com.streak.app.domain.usecase

import com.streak.app.domain.model.Task
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.TaskRepository
import javax.inject.Inject

// Odkljuka ali vrne nalogo; opravljena nima vec opomnikov
class SetTaskDoneUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val reminders: TaskReminderScheduler
) {
    suspend operator fun invoke(task: Task, done: Boolean) {
        repository.setDone(task.id, done)
        reminders.schedule(task.copy(isDone = done))
    }
}
