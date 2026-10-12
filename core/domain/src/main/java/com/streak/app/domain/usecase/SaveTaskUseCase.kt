package com.streak.app.domain.usecase

import com.streak.app.domain.model.Task
import com.streak.app.domain.notify.TaskReminderScheduler
import com.streak.app.domain.repository.TaskRepository
import javax.inject.Inject

// Shrani nalogo in na novo razporedi njene opomnike
class SaveTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val reminders: TaskReminderScheduler
) {
    suspend operator fun invoke(task: Task) {
        repository.upsertTask(task)
        reminders.schedule(task)
    }
}
