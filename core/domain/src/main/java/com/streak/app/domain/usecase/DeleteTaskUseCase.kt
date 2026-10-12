package com.streak.app.domain.usecase

import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val reminders: TaskReminderScheduler
) {
    suspend operator fun invoke(taskId: String) {
        reminders.cancel(taskId)
        repository.deleteTask(taskId)
    }
}
